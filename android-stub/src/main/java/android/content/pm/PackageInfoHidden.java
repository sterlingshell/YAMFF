package android.content.pm;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(PackageInfo.class)
public interface PackageInfoHidden {
    // Usually fields in PackageInfo are public, but we can use this for consistent access if needed.
    // Signature[] signatures;
    // SigningInfo signingInfo;
}
