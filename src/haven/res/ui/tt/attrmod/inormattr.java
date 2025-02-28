/* Preprocessed source code */
package haven.res.ui.tt.attrmod;

import haven.*;
import static haven.PUtils.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.BufferedImage;

@Resource.PublishedCode(name = "attrmod")
public class inormattr extends resattr {
    public final int dec;

    public inormattr(Resource res, Object... args) {
	super(res);
	dec = (args.length > 0) ? Utils.iv(args[0]) : 1;
    }

    public String format(double val) {
	double Δ = (1.0 / (1.0 + val)) - 1.0;
	return(String.format("%s{%s%s%%}",
			     RichText.Parser.col2a((Δ < 0) ? buff : debuff),
			     (Δ < 0) ? "-" : "+", Utils.odformat2(Math.abs(Δ * 100), dec)));
    }
}

