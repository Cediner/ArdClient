/* Preprocessed source code */
package haven.res.ui.tt.attrmod;

import haven.*;
import static haven.PUtils.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.BufferedImage;

@Resource.PublishedCode(name = "attrmod")
public interface Attribute {
    public static final Color buff = new Color(128, 255, 128);
    public static final Color debuff = new Color(255, 128, 128);

    public String name();
    public BufferedImage icon();
    public String format(double val);

    public static Attribute get(Resource res) {
	Attribute attr = res.getcode(Attribute.class, false);
	if(attr == null)
	    attr = new intattr(res);
	return(attr);
    }
}

