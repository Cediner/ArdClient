/* Preprocessed source code */
package haven.res.ui.tt.attrmod;

import haven.*;
import static haven.PUtils.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.BufferedImage;

@Resource.PublishedCode(name = "attrmod")
public abstract class Entry {
    public final Attribute attr;

    public Entry(Attribute attr) {
	this.attr = attr;
    }

    public abstract String fmtvalue();
}

