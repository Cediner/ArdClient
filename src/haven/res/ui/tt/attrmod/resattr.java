/* Preprocessed source code */
package haven.res.ui.tt.attrmod;

import haven.*;
import static haven.PUtils.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.BufferedImage;

@Resource.PublishedCode(name = "attrmod")
public abstract class resattr implements Attribute {
    public final Resource res;

    public resattr(Resource res) {
	this.res = res;
    }

    public BufferedImage icon() {
	Resource.Image img = res.layer(Resource.imgc);
	return((img == null) ? null : img.img);
    }

    public String name() {
	return(res.flayer(Resource.tooltip).t);
    }
}

