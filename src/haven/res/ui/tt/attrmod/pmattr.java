/* Preprocessed source code */
package haven.res.ui.tt.attrmod;

import haven.*;
import static haven.PUtils.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.BufferedImage;

@Resource.PublishedCode(name = "attrmod")
public class pmattr extends resattr {
    public final int dec;

    public pmattr(Resource res, Object... args) {
	super(res);
	dec = (args.length > 0) ? Utils.iv(args[0]) : 1;
    }

    public String format(double val) {
	return(String.format("%s{%s%s%%}",
			     RichText.Parser.col2a((val < 0) ? debuff : buff),
			     (val < 0) ? "-" : "+", Utils.odformat2(Math.abs(val) * 0.1, dec)));
    }
}
