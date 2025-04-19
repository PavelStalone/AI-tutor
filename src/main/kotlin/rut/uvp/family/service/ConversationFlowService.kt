package rut.uvp.family.service

import org.springframework.stereotype.Service
import rut.uvp.family.model.FamilyLeisureRequest
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Qualifier
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

@Service
class ConversationFlowService(
    @Qualifier("GenerationClient") private val chatClient: ChatClient
) {
    private val objectMapper = jacksonObjectMapper()

    fun parseUserMessage(message: String): FamilyLeisureRequest {
        val prompt = """
        Ты — интеллектуальный ассистент мирового класса для подбора семейного досуга и интеграции с KudaGo API. Твоя задача:
        1. Внимательно проанализируй текст запроса пользователя. Используй chain-of-thought:
        - Определи участников (имя, роль, возраст, если есть)
        - Определи дату/день недели (или пойми, что требуется автоподбор)
        - Выдели предпочтения (интересы, типы мероприятий)
        - Выдели ограничения (например, аллергии, бюджет, локация)
        - Определи город (если не указан — уточни)
        2. Сформируй JSON строго по схеме:
        {
          \"members\": [{\"role\": \"мама\", \"age\": 35, \"userId\": 123, \"relation\": \"жена\"}],
          \"date\": \"2024-06-15\" или \"auto\",
          \"preferences\": [\"театр\", \"детские\"],
          \"restrictions\": [\"без животных\"],
          \"city\": \"Москва\",
          \"userId\": 123
        }
        3. Если каких-то данных не хватает, обязательно добавь поле \"missing_fields\" — массив с названиями недостающих полей (например, [\"date\", \"city\"]).
        4. Не добавляй лишних полей, не меняй структуру. Не пиши пояснений, только JSON.
        5. Если есть сомнения — делай предположения, но явно помечай их в missing_fields.
        6. После основного JSON, с новой строки, сгенерируй JSON с параметрами для запроса к KudaGo API по следующей схеме:
        {
          \"location\": \"msk\", // если город Москва, то msk; если Санкт-Петербург — spb; и т.д. (см. документацию KudaGo)
          \"actual_since\": \"1713484800\", // UNIX timestamp для даты (или текущей даты, если auto)
          \"lang\": \"ru\",
          \"fields\": \"title,description,dates,images\",
          \"expand\": \"images,dates\",
          \"page\": \"1\",
          \"page_size\": \"4\"
        }
        Пример:
        Вход: \"Что поделать с дочкой 6 лет в эти выходные в Москве?\"
        Выход:
        {
          \"members\": [{\"role\": \"дочка\", \"age\": 6}],
          \"date\": \"auto\",
          \"preferences\": [],
          \"restrictions\": [],
          \"city\": \"Москва\"
        }
        {
          \"location\": \"msk\",
          \"actual_since\": \"1713484800\",
          \"lang\": \"ru\",
          \"fields\": \"title,description,dates,images\",
          \"expand\": \"images,dates\",
          \"page\": \"1\",
          \"page_size\": \"4\"
        }
        Пользователь: $message
        """
        val response = chatClient.prompt(prompt).call().content()
        val cleanJson = response
            ?.replace(Regex("^```json\\s*"), "") // убираем ```json в начале
            ?.replace(Regex("^```\\s*"), "")    // убираем просто ```
            ?.replace(Regex("```\\s*$"), "")    // убираем ``` в конце
            ?.trim()
        return try {
            objectMapper.readValue<FamilyLeisureRequest>(cleanJson!!)
        } catch (e: Exception) {
            throw IllegalArgumentException("Ошибка парсинга запроса: $response", e)
        }
    }
}