/* Preprocessed source code */
package haven.res.ui.tt.attrmod;

import haven.*;
import static haven.PUtils.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.BufferedImage;

@Resource.PublishedCode(name = "attrmod")
public class intattr extends resattr {
    public intattr(Resource res) {
	super(res);
    }

    public String format(double val) {
	return(String.format("%s{%s%d}",
			     RichText.Parser.col2a((val < 0) ? debuff : buff),
			     (val < 0) ? "-" : "+", Math.abs((int)val)));
    }
}

