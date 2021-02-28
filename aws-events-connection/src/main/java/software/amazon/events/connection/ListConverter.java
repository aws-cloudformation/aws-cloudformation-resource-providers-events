package software.amazon.events.connection;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central location for how lists are handled when translating to/from
 * the resource model and SDK
 */
public class ListConverter {

    // When we translate to an SDK list, we can just check for null for determining if
    // we should return null. No special logic is needed for an empty list as the
    // resource model does not initialize fields of type List<String> to the empty list.
    public static <I, O> List<O> toSdk(List<I> list, Function<I, O> func) {
        if (list == null) {
            return null;
        }
        return list.stream().map(x -> func.apply(x)).collect(Collectors.toList());
    }

    // When we translate to a resource model from an SDK object, check if the list is null OR if it's empty
    // for knowing if we should return null.
    public static <I, O> List<O> toModel(List<I> list, Function<I, O> func) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream().map(x -> func.apply(x)).collect(Collectors.toList());
    }
}
