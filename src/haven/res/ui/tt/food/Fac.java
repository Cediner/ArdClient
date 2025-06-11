/* Preprocessed source code */
package haven.res.ui.tt.food;

import haven.*;
import java.awt.Color;
import java.util.*;
import java.awt.image.*;
import static haven.resutil.FoodInfo.Effect;
import static haven.resutil.FoodInfo.Event;

/* >tt: Fac */
@haven.FromResource(name = "ui/tt/food", version = 14, override = true)
public class Fac implements ItemInfo.InfoFactory {
    public ItemInfo build(ItemInfo.Owner owner, ItemInfo.Raw raw, Object... args) {
        int c = 1;
        double end = Utils.dv(args[c++]);
        double glut = Utils.dv(args[c++]);
        double sev = (args[c] instanceof Number) ? Utils.dv(args[c++]) : 0;
        double cons = (args[c] instanceof Number) ? Utils.dv(args[c++]) : 0;
        Object[] evd = (Object[])args[c++];
        Object[] efd = (Object[])args[c++];
        Object[] tpd = (Object[])args[c++];

        Collection<Event> evs = new LinkedList<Event>();
        Collection<Effect> efs = new LinkedList<Effect>();
        Resource.Resolver rr = owner.context(Resource.Resolver.class);
        for(int a = 0; a < evd.length; a += 2)
            evs.add(new Event(rr.getres((Integer)evd[a]).get(),
                    ((Number)evd[a + 1]).doubleValue()));
        for(int a = 0; a < efd.length; a += 2)
            efs.add(new Effect(ItemInfo.buildinfo(owner, new Object[] {(Object[])efd[a]}),
                    ((Number)efd[a + 1]).doubleValue()));

        int[] types;
        {
            int[] buf = new int[tpd.length * 32];
            int n = 0, t = 0;
            for(int i = 0; i < tpd.length; i++) {
                for(int b = 0, m = 1; b < 32; b++, m <<= 1, t++) {
                    if(((Integer)tpd[i] & m) != 0)
                        buf[n++] = t;
                }
            }
            types = new int[n];
            for(int i = 0; i < n; i++)
                types[i] = buf[i];
        }

        try {
            return(new haven.resutil.FoodInfo(owner, end, glut, cons, sev, evs.toArray(new Event[0]), efs.toArray(new Effect[0]), types));
        } catch(NoSuchMethodError e) {
            return(new haven.resutil.FoodInfo(owner, end, glut, cons, evs.toArray(new Event[0]), efs.toArray(new Effect[0]), types));
        }
    }
}
