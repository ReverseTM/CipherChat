package ru.mai.khasanov.cipherchat.model.message;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Base64;

import static org.apache.commons.codec.binary.Base64.isBase64;

public class KafkaMessageAdapter implements JsonSerializer<KafkaMessage>, JsonDeserializer<KafkaMessage> {
    @Override
    public JsonElement serialize(KafkaMessage message, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("action", message.action().name());
        if (message.content() instanceof Long) {
            jsonObject.addProperty("content", (Long) message.content());
        } else if (message.content() instanceof byte[]) {
            String base64Content = Base64.getEncoder().encodeToString((byte[]) message.content());
            jsonObject.addProperty("content", base64Content);
        } else if (message.content() instanceof ContentMessage) {
            jsonObject.add("content", context.serialize(message.content(), ContentMessage.class));
        } else {
            jsonObject.add("content", context.serialize(message.content()));
        }
        return jsonObject;
    }

    @Override
    public KafkaMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        KafkaMessage.Action action = KafkaMessage.Action.valueOf(jsonObject.get("action").getAsString());
        JsonElement contentElement = jsonObject.get("content");

        Object content;
        if (contentElement.isJsonPrimitive() && contentElement.getAsJsonPrimitive().isNumber()) {
            content = contentElement.getAsLong();
        } else if (contentElement.isJsonPrimitive() && contentElement.getAsJsonPrimitive().isString()) {
            String contentString = contentElement.getAsString();
            if (isBase64(contentString)) {
                content = Base64.getDecoder().decode(contentString);
            } else {
                content = contentString;
            }
        } else if (contentElement.isJsonObject() && contentElement.getAsJsonObject().has("messageType")) {
            content = context.deserialize(contentElement, ContentMessage.class);
        } else {
            content = context.deserialize(contentElement, Object.class);
        }
        return new KafkaMessage(action, content);
    }
}
