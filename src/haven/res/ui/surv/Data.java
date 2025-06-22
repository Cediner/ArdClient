/* Preprocessed source code */
package haven.res.ui.surv;

import haven.Area;
import haven.Message;
import haven.MessageBuf;
import haven.ZMessage;

@haven.FromResource(name = "ui/surv", version = 44, override = true)
public class Data {
    public final Area varea;
    public final float gran;
    public final float[] wz;
    public final int[] dz;
    public int lo, hi;
    public int seq = 0;

    public Data(Area varea, float gran) {
        this.varea = varea;
        this.gran = gran;
        this.wz = new float[varea.area()];
        this.dz = new int[varea.area()];
    }

    public void eupdate() {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int z : this.dz) {
            lo = Math.min(lo, z);
            hi = Math.max(hi, z);
        }
        this.lo = lo;
        this.hi = hi;
    }

    public void dupdate() {
        for (int i = 0; i < wz.length; i++)
            dz[i] = Math.round(wz[i]);
    }

    public void decode(int base, byte[] enc) {
        Message zd = new ZMessage(new MessageBuf(enc));
        for (int i = 0; i < wz.length; i++)
            wz[i] = base + zd.uint8();
        for (int i = 0; i < wz.length; i++)
            dz[i] = Math.round(wz[i]);
    }

    public Object[] encode() {
        eupdate();
        MessageBuf buf = new MessageBuf();
        ZMessage out = new ZMessage(buf);
        for (int i = 0; i < dz.length; i++)
            out.adduint8(dz[i] - lo);
        out.finish();
        return (new Object[]{lo, buf.fin()});
    }
}
