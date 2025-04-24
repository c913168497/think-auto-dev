package think.auto.dev.llm.provider

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.theokanning.openai.client.OpenAiApi
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.completion.chat.ChatMessageRole
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.service.OpenAiService.defaultObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import think.auto.dev.llm.LLMProvider
import think.auto.dev.llm.history.LLMHistoryEmptyRecording
import think.auto.dev.llm.history.LLMHistoryJsonRecording
import think.auto.dev.llm.history.LLMHistoryRecording
import think.auto.dev.llm.history.LLMHistoryRecordingInstruction
import think.auto.dev.llm.role.ChatRole
import think.auto.dev.settings.aiProvider.AiProvider
import java.time.Duration

//@Service(Service.Level.PROJECT)
class OpenAIProvider(val project: Project, val aiProvider: AiProvider) : LLMProvider {
    private val timeout = Duration.ofSeconds(defaultTimeout)
    private val openAiModel: String = aiProvider.customModel;
    private val openAiToken: String = aiProvider.customEngineToken
    private var openAiHost: String = aiProvider.customOpenAiHost
    private var openAiHostHead: String = aiProvider.customEngineHead
    private val historyChatMessages: MutableList<ChatMessage> = ArrayList()
    private var historyMessageLength: Int = 0

    private val service: OpenAiService = initOpenAiService()

    private val llmHistoryRecording: LLMHistoryRecording = initHistoryRecording()

    private fun initHistoryRecording(): LLMHistoryRecording {
        if (aiProvider.recordingInLocal) {
            return project.service<LLMHistoryJsonRecording>()
        }

        return LLMHistoryEmptyRecording()
    }

    private fun initOpenAiService(): OpenAiService {
        if (openAiToken.isEmpty()) {
            throw IllegalStateException("You LLM server Key is empty")
        }

        if (!openAiHost.endsWith("/")) {
            openAiHost += "/"
        }
        val headValues = openAiHostHead.split(",")

        val mapper = defaultObjectMapper()
        val client = defaultClient(openAiToken, timeout, headValues)
        val retrofit = Retrofit.Builder().baseUrl(openAiHost).client(client)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build()
        val api = retrofit.create(OpenAiApi::class.java)
        return OpenAiService(api)
    }

    /**
     * 默认客户端
     */
    private fun defaultClient(openAiToken: String, timeout: Duration, headValues: List<String>): OkHttpClient {
        return OkHttpClient.Builder().connectTimeout(timeout).readTimeout(timeout).writeTimeout(timeout)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                // 添加固定 Headers（如 Authorization）
                requestBuilder.header("Authorization", "Bearer $openAiToken")

                // 解析 headValues 并动态添加 Headers
                if (headValues.isNotEmpty()) {
                    headValues.forEach { header ->
                        if (header.isNotBlank()) {
                            val (key, value) = header.split(":", limit = 2) // 按 ":" 分割成 key 和 value
                            requestBuilder.header(key.trim(), value.trim())  // 去除前后空格
                        }
                    }
                }

                chain.proceed(requestBuilder.build())
            }.build()
    }

    override fun clearMessage() {
        historyChatMessages.clear()
        historyMessageLength = 0
    }

    override fun appendLocalMessage(msg: String, role: ChatRole) {
        val chatMessage = ChatMessage(role.roleName(), msg)
        historyChatMessages.add(chatMessage)
    }

    override fun prompt(promptText: String): String {
        val completionRequest = prepareRequest(promptText, "", true)

        val completion = service.createChatCompletion(completionRequest)
        val output = completion.choices[0].message.content

        return output
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean): Flow<String> {
        if (!keepHistory) {
            clearMessage()
        }

        var output = ""
        val completionRequest = prepareRequest(promptText, systemPrompt, keepHistory)

        return callbackFlow {
            withContext(Dispatchers.IO) {
                service.streamChatCompletion(completionRequest).doOnError { error ->
                    logger.error("Error in stream", error)
                    trySend(error.message ?: "Error occurs")
                }.blockingForEach { response ->
                    if (response.choices.isNotEmpty()) {
                        val completion = response.choices[0].message
                        if (completion != null && completion.content != null) {
                            output += completion.content
                            trySend(completion.content)
                        }
                    }
                }

                llmHistoryRecording.write(LLMHistoryRecordingInstruction(promptText, output))
                historyChatMessages.add(ChatMessage(ChatMessageRole.ASSISTANT.value(), output))
                if ((!keepHistory)) {
                    clearMessage()
                }

                close()
            }
        }
    }

    private fun prepareRequest(promptText: String, systemPrompt: String, keepHistory: Boolean): ChatCompletionRequest? {
        if (systemPrompt.isEmpty()) {
            val systemMessage = ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt)
            historyChatMessages.add(systemMessage)
        }

        val userMessage = ChatMessage(ChatMessageRole.USER.value(), promptText)

        historyMessageLength += promptText.length
        historyChatMessages.add(userMessage)
        logger.info("messages length: ${historyChatMessages.size}")
        val chatCompletionRequest =
            ChatCompletionRequest.builder().model(openAiModel).temperature(0.0).messages(historyChatMessages).build()

        return chatCompletionRequest
    }

    companion object {
        private val logger: Logger = logger<OpenAIProvider>()
    }
}
