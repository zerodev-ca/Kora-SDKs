package ca.zerodev.kora;

import com.google.gson.JsonObject;
import java.util.List;

public record KoraPage(int total, List<JsonObject> rows) {
}
