package dev.konqasasas.beat.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;

public final class JsonSupport {
    private JsonSupport() {
    }

    public static Gson createGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .setStrictness(Strictness.STRICT)
                .disableJdkUnsafe()
                .create();
    }
}
