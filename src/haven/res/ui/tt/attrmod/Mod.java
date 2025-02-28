/* Preprocessed source code */
package haven.res.ui.tt.attrmod;

import haven.*;
import static haven.PUtils.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.BufferedImage;

@Resource.PublishedCode(name = "attrmod")
public class Mod extends Entry {
    public final double mod;

    public Mod(Attribute attr, double mod) {
	super(attr);
	this.mod = mod;
    }

    public String fmtvalue() {
	return(attr.format(mod));
    }
}

/* >tt: AttrMod$Fac */
