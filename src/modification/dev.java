package modification;

import haven.CheckListbox;
import haven.CheckListboxItem;
import haven.Debug;
import haven.GItem;
import haven.Resource;
import haven.UI;
import haven.Utils;
import haven.Widget;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class dev {
    public static boolean logging = Utils.getprefb("msglogging", false);      //allow log in console
    public static boolean loadLog = false;
    public static boolean decodeCode = Utils.getprefb("decodeCode", false);
    public static boolean skipexceptions = Utils.getprefb("skipexceptions", false);
    public static boolean reslog = Utils.getprefb("reslog", false);

    public static boolean msg_log_skip_boolean = Utils.getprefb("skiplogmsg", false);
    public static List<String> msg_log_skip = new ArrayList<>();

    public static CheckListbox msglist = null;
    public static final Map<String, CheckListboxItem> msgmenus = new TreeMap<>();

    static {
        msg_log_skip.addAll(Arrays.asList("glut", "click"));
        Utils.loadcollection("msgcollection").forEach(msg -> msgmenus.put(msg, new CheckListboxItem(msg)));
    }

    public static void addMsg(String name) {
        List<String> list = new ArrayList<>(msgmenus.keySet());
        if (msgmenus.get(name) == null) {
            list.add(name);
            CheckListboxItem ci = new CheckListboxItem(name);
            msgmenus.put(name, ci);
            if (msglist != null)
                msglist.items.add(ci);
            Utils.setcollection("msgcollection", msgmenus.keySet());
        }
    }

    public static String serverSender = "_SERVER_MSG";
    public static String clientSender = "_CLIENT_MSG";

    public static String localTime() {
        return (LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
    }

    public static void simpleLog(Throwable e) {
        if (logging) {
            StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(LocalTime.now()).append("]").append(" || ").append("ERROR").append(" || ").append(stackTraceElements[1]).append(" || ").append(e.getMessage());

            System.err.println(sb.toString());
        }
        e.printStackTrace();
        Debug.printStackTrace(e);
    }

    public static void simpleLog(String e) {
        if (logging) {
            StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(localTime()).append("]").append(" || ").append("INFO").append(" || ").append(stackTraceElements[1]).append(" || ").append(e);

            System.err.println(sb.toString());
        }
        Debug.println(e);
    }

    public static void simpleLog(String s, Throwable e) {
        if (logging) {
            StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(localTime()).append("]").append(" || ").append("ERROR").append(" || ").append(stackTraceElements[1]).append(" || ").append(s).append(" || ").append(e.getMessage());

            System.err.println(sb.toString());
        }
        e.printStackTrace();
        Debug.println(s);
        Debug.printStackTrace(e);
    }

    public static void sysPrintStackTrace(String text) {
        if (logging) {
            StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
            int stackTraceElementsLength = stackTraceElements.length;

            System.out.print(text + " || ");
            for (int i = 1; i < stackTraceElementsLength; i++) {
                System.out.print("{" + stackTraceElements[i].getClassName() + "(" + stackTraceElements[i].getMethodName() + ")(" + stackTraceElements[i].getLineNumber() + ")");
                if (i != stackTraceElementsLength - 1) System.out.print(">");
            }

            System.out.println();
        }
    }

    public static void resourceLog(Object... strings) {
        if (logging) {
            for (Object s : strings) {
                if (s instanceof Object[]) {
                    System.out.print(" || ");
                    argsMethod((Object[]) s);
                } else
                    System.out.print(s == null ? "null" : s.toString() + " ");
            }
            System.out.println();
        }
    }

    public static void sysLog(String who, Widget widget, int id, String msg, Object... args) {
        boolean skip_log = false;
        for (Map.Entry<String, CheckListboxItem> entry : msgmenus.entrySet()) {
            if (msg_log_skip_boolean && entry.getKey().toLowerCase().equals(msg) && entry.getValue().selected)
                skip_log = true;
        }

        if (!skip_log && logging) {
            System.out.print("[" + localTime() + "]");

            System.out.print(" || " + who);
            if (widget.ui != null && widget.ui.sess != null) System.out.print("[" + widget.ui.sess.username + "]");

            System.out.print(" || " + widget + "(" + id + ")");
            if (widget instanceof GItem) {
                try {
                    Resource res = ((GItem) widget).getres();
                    System.out.print("[" + res + "]");
                } catch (Throwable e) {
                    System.out.print(e);
                }
            }

            int a;
            if (id == -1) a = 1;
            else if ((id / 10) == 0) a = 0;
            else if ((id / 10) < 10) a = 1;
            else a = 2;

            System.out.print(" || " + msg);
            addMsg(msg);

            System.out.print(" || ");
            argsMethod(args);
            System.out.println();
        }
    }

    public static void sysLogRemote(String who, Widget widget, int id, String type, int parent, Object[] pargs, Object... cargs) {
        UI ui = null;
        if (widget.ui != null) ui = widget.ui;

        boolean skip_log = false;

        if (!skip_log && logging) {
            System.out.print("[" + localTime() + "]");
//            System.out.print(" || ");
//            for (int i = 1; i < stackTraceElementsLength; i++) {
//                System.out.print("{" + stackTraceElements[i].getClassName() + "(" + stackTraceElements[i].getMethodName() + ")(" + stackTraceElements[i].getLineNumber() + ")");
//                if (i != stackTraceElementsLength - 1) System.out.print(">");
//            }
            System.out.print(" || " + who);
            if (widget.ui != null && widget.ui.sess != null) System.out.print("[" + widget.ui.sess.username + "]");

            System.out.print(" || " + widget + "(" + id + ")");
            if (widget instanceof GItem) {
                try {
                    Resource res = ((GItem) widget).getres();
                    System.out.print("[" + res + "]");
                } catch (Throwable e) {
                    System.out.print(e);
                }
            }

//            if (name != null) System.out.print(" || " + name);
            if (type != null) System.out.print(" || " + type);
            if (parent != -1) {
                System.out.print(" || ");
                if (ui != null)
                    System.out.print(widget.ui.widgets.get(parent) + "(");
                System.out.print(parent);
                if (ui != null)
                    System.out.print(")");
            }

            if (pargs != null)
                System.out.print(" || ");
            argsMethod(pargs);
            if (cargs != null)
                System.out.print(" || ");
            argsMethod(cargs);

            System.out.println();
        }
    }

    private static void argsMethod(Object[] pargs) {
        if (pargs != null) {
            System.out.print("[" + pargs.length + "]:");
            for (int i = 0; i < pargs.length; i++) {
                if (pargs[i] instanceof Object[]) {
                    System.out.print("{");
                    argsMethod((Object[]) pargs[i]);
                    System.out.print("}");
                } else
                    System.out.print("[" + pargs[i] + "]");
            }
        }
    }

    public static void checkFileVersion(String resname, int curver) {
        if (true) return;
        try {
            Resource res = Resource.remote().loadwait(resname);

            if (res == null) {
                System.out.printf("[i] Resource [%s(%d)] not found!", resname, curver);
                return;
            }
            if (curver != res.ver)
                System.out.printf("[i] Resource [%s] (old %d). Please update!", res, curver);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static String mapToString(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            sb.append("[").append(entry.getKey()).append(",").append(entry.getValue()).append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String collectionToString(Collection<?> collection) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            sb.append("[").append(it.next()).append("]");
        }
        sb.append("]");
        return sb.toString();
    }
}
