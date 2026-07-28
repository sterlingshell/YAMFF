package android.content.pm;

import java.util.List;
import dev.rikka.tools.refine.RefineAs;

@RefineAs(ParceledListSlice.class)
public interface ParceledListSliceHidden<T> {
    List<T> getList();
}
