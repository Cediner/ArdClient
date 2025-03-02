package haven.res.lib.dynspr;

import haven.GAttrib;
import haven.Gob;
import haven.Message;
import haven.UID;

import java.util.ArrayList;

public class Dynamic extends GAttrib {
    public final UID[] ids;

    public Dynamic(Gob gob, UID[] ids) {
        super(gob);
        this.ids = ids;
    }

    public static void parse(Gob gob, Message sdt) {
        ArrayList<UID> buf = new ArrayList<>();
        while (!sdt.eom())
            buf.add(sdt.uniqid());
        if (buf.isEmpty())
            gob.delattr(Dynamic.class);
        gob.setattr(new Dynamic(gob, buf.toArray(new UID[0])));
    }
}
