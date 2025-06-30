package haven.automation;

import haven.Area;
import haven.Button;
import haven.CheckBox;
import haven.CheckListbox;
import haven.CheckListboxItem;
import haven.Config;
import haven.Coord;
import haven.Coord2d;
import haven.Dropbox;
import haven.FlowerMenu;
import haven.GItem;
import haven.GOut;
import haven.GameUI;
import haven.ISBox;
import haven.Inventory;
import haven.Label;
import haven.ResizableTextEntry;
import haven.Resource;
import haven.Scrollbar;
import haven.Tex;
import haven.Text;
import haven.TextEntry;
import haven.Trinity;
import haven.UI;
import haven.WItem;
import haven.Widget;
import haven.WidgetVerticalAppender;
import haven.Window;
import haven.purus.DrinkWater;
import haven.purus.pbot.PBotCharacterAPI;
import haven.purus.pbot.PBotGob;
import haven.purus.pbot.PBotGobAPI;
import haven.purus.pbot.PBotInventory;
import haven.purus.pbot.PBotItem;
import haven.purus.pbot.PBotUtils;
import haven.purus.pbot.PBotWindowAPI;
import haven.sloth.script.pathfinding.Hitbox;
import modification.configuration;
import modification.resources;
import org.apache.commons.collections4.list.TreeList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * (Beta)
 * Preset = file (JSON)
 * Endless = time
 * Professional options (drink . dead option . wait time . retry .)
 * Stop btn, pause btn
 * Storages like barrel
 * Action like destroy
 * <p>
 * Main Info + Refresh Button (FlowerMenu)
 * 1. Select area and select gobs . add option check once (not until it disappears) . moving objects? . automilk?
 * 2. FlowerMenu (without flowermenu = just right click?) . favorite? . disable popup . sequence (1 2 3) . filter selected gobs (auto click all gobs and check)
 * 3. optional. Storage...
 * 4. Storage : select area and chests
 * Drink water? click block for running
 * <p>
 * <p>
 * final. Run
 */

public class AreaPicker extends Window implements Runnable {
    public static final String scriptname = "Area Picker";
    public static final String[] collectstates = new String[]{"Inventory", "Drop out of hand", "Storage", "Create Stockpiles (WIP)"};
    public static final String ACTION_START = "Action:";
    public static final Pattern ACTION_PATTERN = Pattern.compile(ACTION_START + "(\\w+)");
    public static final Map<String, String> actions1 = haven.Utils.<String, String>map().put("Inspect", "inspect").put("Lift", "carry").map();
    public static final Map<String, String> actions2 = haven.Utils.<String, String>map().put("Repair", "repair").put("Destroy", "destroy").map();
    public final WidgetVerticalAppender appender = new WidgetVerticalAppender(this);
    public Thread runthread;
    public boolean block = false;
    public String barrel; //gfx/terobjs/barrel
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();

    public Area gobarea, storagearea;
    public final List<String>
            areagoblist = new ArrayList<>(),
            flowermenulist = new ArrayList<>(Config.flowermenus.keySet()),
            collecttrigger = new ArrayList<>(Arrays.asList(collectstates)),
            areastoragelist = new ArrayList<>(),
            addeditemlist = new ArrayList<>();
    public final List<PBotGob>
            currentgoblist = new ArrayList<>(),
            currentstoragelist = new ArrayList<>();
    public final List<CheckListboxItem>
            selectedgoblist = new TreeList<>(),
            selectedstoragelist = new TreeList<>(),
            selectedflowerlist = new ArrayList<>(),
            selecteditemlist = new TreeList<>();


    public Button refresh, selectgobbtn, selectedgobbtn, selectedflowerbtn, selectstoragebtn, selectedstoragebtn, selecteditembtn, selecteditemaddbtn, runbtn, stopbtn, pausumebtn, loadbtn, savebtn, exportbtn, importbtn;
    public Label l1, l2, l3, l4, l5, l6;
    public Label maininfolbl, areagobinfolbl, flowerpetalsinfolbl, areastorageinfolbl, iteminfolbl;
    public Dropbox<String> collecttriggerdbx;
    public Window selectedgobwnd, selectedflowerwnd, selectedstoragewnd, selecteditemwnd;
    public CheckListbox selectedgoblbox, selectedflowerlbox, selectedstoragelbox, selecteditemlbox;
    public TextEntry waitingtimete, selectedgobsearch, selectedflowersearch, selectedstoragesearch, selecteditemsearch, selecteditemaddtext;
    public CheckBox scancontinuouslycheckbox;

    public int retry = 5;
    public int waitingtime = 1000;

    public AreaPicker() {
        super(Coord.z, scriptname, scriptname);
    }

    @Override
    public void added() {
        super.added();

        maininfolbl = new Label("");
        waitingtimete = new ResizableTextEntry(1000 + "") { //for prof settings
            @Override
            public boolean type(char c, KeyEvent ev) {
                if (c >= KeyEvent.VK_0 && c <= KeyEvent.VK_9 || c == '\b') {
                    return (buf.key(ev));
                } else if (c == '\n') {
                    try {
                        waitingtime = text().equals("") ? 0 : Integer.parseInt(text());
                        return (true);
                    } catch (NumberFormatException ignore) {
                    }
                }
                return (false);
            }
        };

        selectgobbtn = new Button(UI.scale(50), "Select area") {
            {
                change(Color.RED);
            }

            @Override
            public void click() {
                new Thread(new selectinggobarea(), "Selecting Area").start();
            }

            @Override
            public boolean mousedown(Coord c, int btn) {
                if (isblocked()) return (true);
                else return (super.mousedown(c, btn));
            }
        };
        selectedgobbtn = new Button(UI.scale(50), "Gob list") {
            {
                change(Color.RED);
            }

            @Override
            public void click() {
                if (selectedgoblist.isEmpty()) {
                    debugLogPing("Select area with objects", Color.WHITE);
                } else {
                    if (!ui.gui.containschild(selectedgobwnd))
                        ui.gui.add(selectedgobwnd, c.add(parent.c));
                    else
                        selectedgobwnd.unlink();
                }
            }
        };
        selectedgobwnd = new Window(Coord.z, "Selecting gob") {
            {
                WidgetVerticalAppender wva = new WidgetVerticalAppender(this);
                selectedgoblbox = new CheckListbox(UI.scale(100), 10) {
                    @Override
                    protected void itemclick(CheckListboxItem itm, int button) {
                        if (!isblocked()) {
                            super.itemclick(itm, button);
                            for (CheckListboxItem i : selectedgoblist) {
                                if (i.name.equals(itm.name)) {
                                    i.selected = itm.selected;
                                    break;
                                }
                            }
                            items.sort(listboxsort());
                            updatelist("gob");
                            updateinfo("gob");
                        }
                    }

                    @Override
                    protected void drawitemname(GOut g, CheckListboxItem itm) {
                        Text t = Text.render(configuration.getShortName(itm.name) + " (" + itm.name.substring(0, itm.name.lastIndexOf('/')) + ")");
                        Tex T = t.tex();
                        g.image(T, UI.scale(2, 2), t.sz());
                        T.dispose();
                    }
                };
                selectedgobsearch = new ResizableTextEntry(selectedgoblbox.sz.x, "") {
                    @Override
                    public void changed() {
                        update();
                    }

                    @Override
                    public boolean mousedown(Coord mc, int btn) {
                        if (btn == 3) {
                            settext("");
                            update();
                            return (true);
                        } else {
                            return (super.mousedown(mc, btn));
                        }
                    }

                    public void update() {
                        selectedgoblbox.items.clear();
                        for (CheckListboxItem i : selectedgoblist) {
                            if (i.name.toLowerCase().contains(text().toLowerCase()))
                                selectedgoblbox.items.add(i);
                        }
                        selectedgoblbox.items.sort(listboxsort());
                    }
                };
                wva.add(selectedgoblbox);
                wva.add(selectedgobsearch);
                pack();
            }

            @Override
            public void reqdestroy() {
                unlink();
            }
        };
        areagobinfolbl = new Label("");

        selectedflowerbtn = new Button(UI.scale(50), "Flower list") {
            @Override
            public void click() {
                if (!ui.gui.containschild(selectedflowerwnd))
                    ui.gui.add(selectedflowerwnd, c.add(parent.c));
                else
                    selectedflowerwnd.unlink();
            }
        };
        selectedflowerwnd = new Window(Coord.z, "Selecting petals") {
            {
                WidgetVerticalAppender wva = new WidgetVerticalAppender(this);
                actions1.forEach((k, v) -> selectedflowerlist.add(new CheckListboxItem(ACTION_START + k)));
                actions2.forEach((k, v) -> selectedflowerlist.add(new CheckListboxItem(ACTION_START + k)));
                flowermenulist.forEach(i -> selectedflowerlist.add(new CheckListboxItem(i)));
                final List<String> temp = new ArrayList<>();
                flowermenulist.forEach(s -> {
                    String loc = Resource.language.equals("en") ? s : Resource.getLocString(Resource.BUNDLE_FLOWER, s);
                    temp.add(Resource.language.equals("en") ? s : loc.equals(s) ? s : s + " (" + Resource.getLocString(Resource.BUNDLE_FLOWER, s) + ")");
                });
                selectedflowerlbox = new CheckListbox(calcWidthString(temp), 10) {
                    @Override
                    protected void itemclick(CheckListboxItem itm, int button) {
                        if (!isblocked()) {
                            super.itemclick(itm, button);
                            for (CheckListboxItem i : selectedflowerlist) {
                                if (i.name.equals(itm.name)) {
                                    i.selected = itm.selected;
                                    break;
                                }
                            }
                            items.sort(listboxsort());
                            updateinfo("flower");
                        }
                    }

                    @Override
                    protected void drawitemname(GOut g, CheckListboxItem itm) {
                        String loc = Resource.language.equals("en") ? itm.name : Resource.getLocString(Resource.BUNDLE_FLOWER, itm.name);
                        Tex t = Text.render(Resource.language.equals("en") ? itm.name : loc.equals(itm.name) ? itm.name : itm.name + " (" + Resource.getLocString(Resource.BUNDLE_FLOWER, itm.name) + ")").tex();
                        g.image(t, UI.scale(2, 2), t.sz());
                        t.dispose();
                    }
                };
                selectedflowerlbox.items.addAll(selectedflowerlist);
                selectedflowerlbox.items.sort(listboxsort());
                selectedflowersearch = new ResizableTextEntry(selectedflowerlbox.sz.x, "") {
                    @Override
                    public void changed() {
                        update();
                    }

                    @Override
                    public boolean mousedown(Coord mc, int btn) {
                        if (btn == 3) {
                            settext("");
                            update();
                            return (true);
                        } else {
                            return (super.mousedown(mc, btn));
                        }
                    }

                    public void update() {
                        selectedflowerlbox.items.clear();
                        for (CheckListboxItem i : selectedflowerlist) {
                            String s = Resource.language.equals("en") ? i.name : i.name + " (" + Resource.getLocString(Resource.BUNDLE_FLOWER, i.name) + ")";
                            if (s.toLowerCase().contains(text().toLowerCase()))
                                selectedflowerlbox.items.add(i);
                        }
                        selectedflowerlbox.items.sort(listboxsort());
                    }
                };
                wva.add(selectedflowerlbox);
                wva.add(selectedflowersearch);
                pack();
            }

            @Override
            public void reqdestroy() {
                unlink();
            }
        };
        flowerpetalsinfolbl = new Label("");
        updateinfo("flower");

        collecttriggerdbx = new Dropbox<String>(10, collecttrigger) {
            @Override
            protected String listitem(int i) {
                return collecttrigger.get(i);
            }

            @Override
            protected int listitems() {
                return collecttrigger.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(int index) {
                super.change(index);
                String item = sel;
                if (item != null) {
                    int x = Text.render(item).sz().x;
                    if (sz.x != x + Dropbox.drop.sz().x + UI.scale(2)) resize(new Coord(x + Dropbox.drop.sz().x + UI.scale(2), sz.y));
                }
            }

            @Override
            public boolean mousedown(Coord c, int btn) {
                if (!isblocked()) {
                    super.mousedown(c, btn);
                    if (dl != null) resizedl(collecttrigger);
                }
                return (true);
            }
        };
        collecttriggerdbx.selindex = -1;

        selectstoragebtn = new Button(UI.scale(50), "Select area") {
            {
                change(Color.RED);
            }

            @Override
            public void click() {
                new Thread(new selectingstoragearea(), "Selecting Area").start();
            }

            @Override
            public boolean mousedown(Coord c, int btn) {
                if (isblocked()) return (true);
                else return (super.mousedown(c, btn));
            }
        };
        selectedstoragebtn = new Button(UI.scale(50), "Storage list") {
            {
                change(Color.RED);
            }

            @Override
            public void click() {
                if (selectedstoragelist.isEmpty()) {
                    debugLogPing("Select area with objects", Color.WHITE);
                } else {
                    if (!ui.gui.containschild(selectedstoragewnd))
                        ui.gui.add(selectedstoragewnd, c.add(parent.c));
                    else
                        selectedstoragewnd.unlink();
                }
            }
        };
        selectedstoragewnd = new Window(Coord.z, "Selecting storage") {
            {
                WidgetVerticalAppender wva = new WidgetVerticalAppender(this);
                selectedstoragelbox = new CheckListbox(UI.scale(100), 10) {
                    @Override
                    protected void itemclick(CheckListboxItem itm, int button) {
                        if (!isblocked()) {
                            super.itemclick(itm, button);
                            for (CheckListboxItem i : selectedgoblist) {
                                if (i.name.equals(itm.name)) {
                                    i.selected = itm.selected;
                                    break;
                                }
                            }
                            items.sort(listboxsort());
                            updatelist("storage");
                            updateinfo("storage");
                        }
                    }

                    @Override
                    protected void drawitemname(GOut g, CheckListboxItem itm) {
                        Text t = Text.render(configuration.getShortName(itm.name) + " (" + itm.name.substring(0, itm.name.lastIndexOf('/')) + ")");
                        Tex T = t.tex();
                        g.image(T, UI.scale(2, 2), t.sz());
                        T.dispose();
                    }
                };
                selectedstoragesearch = new ResizableTextEntry(selectedstoragelbox.sz.x, "") {
                    @Override
                    public void changed() {
                        update();
                    }

                    @Override
                    public boolean mousedown(Coord mc, int btn) {
                        if (btn == 3) {
                            settext("");
                            update();
                            return (true);
                        } else {
                            return (super.mousedown(mc, btn));
                        }
                    }

                    public void update() {
                        selectedstoragelbox.items.clear();
                        for (CheckListboxItem i : selectedstoragelist) {
                            if (i.name.toLowerCase().contains(text().toLowerCase()))
                                selectedstoragelbox.items.add(i);
                        }
                        selectedstoragelbox.items.sort(listboxsort());
                    }
                };
                wva.add(selectedstoragelbox);
                wva.add(selectedstoragesearch);
                pack();
            }

            @Override
            public void reqdestroy() {
                unlink();
            }
        };
        areastorageinfolbl = new Label("");

        selecteditembtn = new Button(UI.scale(50), "Item list") {
            @Override
            public void click() {
                if (!ui.gui.containschild(selecteditemwnd))
                    ui.gui.add(selecteditemwnd, c.add(parent.c));
                else
                    selecteditemwnd.unlink();
            }
        };
        selecteditemwnd = new Window(Coord.z, "Selecting gob") {
            {
                WidgetVerticalAppender wva = new WidgetVerticalAppender(this);
                stringInInvContent().forEach(i -> selecteditemlist.add(new CheckListboxItem(i)));
                selecteditemlbox = new CheckListbox(UI.scale(100), 10) {
                    @Override
                    protected void itemclick(CheckListboxItem itm, int button) {
                        if (!isblocked()) {
                            if (button == 3) {
                                selecteditemlist.remove(itm);
                                selecteditemlbox.items.remove(itm);
                                addeditemlist.remove(itm.name);
                            } else {
                                super.itemclick(itm, button);
                            }
                            for (CheckListboxItem i : selecteditemlist) {
                                if (i.name.equals(itm.name)) {
                                    i.selected = itm.selected;
                                    break;
                                }
                            }
                            items.sort(listboxsort());
                            updateinfo("item");
                        }
                    }
                };
                selecteditemlbox.items.addAll(selecteditemlist);
                selecteditemlbox.items.sort(listboxsort());
                selecteditemsearch = new ResizableTextEntry(selecteditemlbox.sz.x, "") {
                    @Override
                    public void changed() {
                        update();
                    }

                    @Override
                    public boolean mousedown(Coord mc, int btn) {
                        if (btn == 3) {
                            settext("");
                            update();
                            return (true);
                        } else {
                            return (super.mousedown(mc, btn));
                        }
                    }

                    public void update() {
                        selecteditemlbox.items.clear();
                        for (CheckListboxItem i : selecteditemlist) {
                            if (i.name.toLowerCase().contains(text().toLowerCase()))
                                selecteditemlbox.items.add(i);
                        }
                        selecteditemlbox.items.sort(listboxsort());
                    }

                    @Override
                    public void tick(double dt) {
                        super.tick(dt);
                        if (!isblocked()) {
                            updateitemlist();
                            update();
                            int w = calcWidthCheckListbox(selecteditemlist);
                            if (selecteditemlbox.sz.x != w)
                                selecteditemlbox.resize(w, selectedgoblbox.sz.y);
                            selecteditemwnd.pack();
                        }
                    }
                };
                selecteditemaddtext = new ResizableTextEntry(selecteditemlbox.sz.x / 2, "") {
                    @Override
                    public boolean mousedown(Coord mc, int btn) {
                        if (!isblocked()) {
                            if (btn == 3) {
                                settext("");
                                return (true);
                            }
                        }
                        return (super.mousedown(mc, btn));
                    }

                    @Override
                    public boolean type(char c, KeyEvent ev) {
                        if (!isblocked()) {
                            if (c == '\n' && !text().equals("")) {
                                addeditemlist.add(text());
                            } else {
                                return buf.key(ev);
                            }
                        }
                        return (true);
                    }
                };
                selecteditemaddbtn = new Button(UI.scale(50), "Add") {
                    @Override
                    public void click() {
                        if (!isblocked()) {
                            if (!selecteditemaddtext.text().equals("")) {
                                addeditemlist.add(selecteditemaddtext.text());
                            }
                        }
                    }
                };
                wva.add(selecteditemlbox);
                wva.add(selecteditemsearch);
                wva.addRow(selecteditemaddtext, selecteditemaddbtn);
                pack();
            }

            @Override
            public void reqdestroy() {
                unlink();
            }
        };
        iteminfolbl = new Label("");
        updateinfo("item");

        runbtn = new Button(UI.scale(50), "Run") {
            @Override
            public void click() {
                (runthread = new Thread(AreaPicker.this, "Area Collecting")).start();
            }
        };
        stopbtn = new Button(UI.scale(50), "Stop") {
            @Override
            public void click() {
                try {
                    stop();
                } catch (InterruptedException ignore) {
                }
            }
        };
        pausumebtn = new Button(UI.scale(50), "Pause") {
            @Override
            public void click() {
                if (runthread.isAlive()) {
                    if (runthread.isInterrupted()) {
                        resume();
                    } else {
                        pause();
                    }
                }
            }
        };

        loadbtn = new Button(UI.scale(50), "Load") {
            final List<Trinity<Button, Button, Button>> rows = new ArrayList<>();

            private void acceptTask(final String name, final Object task) {
                if (task instanceof JSONObject) {
                    try {
                        JSONObject object = (JSONObject) task;

                        if (taskFromObject(object)) AreaPicker.this.pack();

                        debugLogPing(String.format("Task %s is loaded", name), Color.GREEN);
                    } catch (Exception e) {
                        debugLogPing(String.format("Error while loading", name), Color.GREEN);
                        e.printStackTrace();
                    }
                }
            }

            private void removeTask(final Window w, final JSONObject object, final String name) {
                if (object.has(name)) {
                    object.remove(name);
                    configuration.savejson("AreaPickerTasks.json", object);
                }
                redrawRows(w, object);
            }

            private void exportTask(final String name, final Object task) {
                if (task instanceof JSONObject) {
                    try {
                        JSONObject object = (JSONObject) task;

                        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                        cb.setContents(new StringSelection(object.toString()), null);

                        if (taskFromObject(object)) AreaPicker.this.pack();

                        debugLogPing(String.format("Task %s is exported to clipboard", name), Color.GREEN);
                    } catch (Exception e) {
                        debugLogPing(String.format("Error %s while exporting", name), Color.GREEN);
                        e.printStackTrace();
                    }
                }
            }

            private void redrawRows(final Window w, final JSONObject object) {
                rows.forEach(row -> {
                    row.a.reqdestroy();
                    row.b.reqdestroy();
                    row.c.reqdestroy();
                });
                rows.clear();

                WidgetVerticalAppender wva = new WidgetVerticalAppender(w);
                for (String name : object.keySet().stream().sorted(String::compareToIgnoreCase).collect(Collectors.toList())) {
                    final Button btn = new Button(UI.scale(50), name, () -> acceptTask(name, object.get(name)));
                    final Button cl = new Button(UI.scale(24), "X", () -> removeTask(w, object, name));
                    final Button exp = new Button(UI.scale(24), "Export", () -> exportTask(name, object.get(name)));
                    exp.settip("Export task to clipboard");
                    rows.add(new Trinity<>(btn, cl, exp));
                    wva.addRow(btn, cl, exp);
                }

                w.pack();
            }

            @Override
            public void click() {
                Optional.ofNullable(getparent(GameUI.class)).ifPresent(gui -> {
                    JSONObject object = resources.areaTasksJson;

                    if (object.length() > 0) {
                        Window w = new Window(Coord.z, "Area Picker Tasks");

                        redrawRows(w, object);

                        gui.add(w, c.add(parent.c));
                    } else {
                        debugLogPing("Tasks not found. Please add first", Color.RED);
                    }
                });
            }
        };
        savebtn = new Button(UI.scale(50), "Save") {
            @Override
            public void click() {
                Optional.ofNullable(getparent(GameUI.class)).ifPresent(gui -> {
                    JSONObject object = resources.areaTasksJson;
                    Window w = new Window(Coord.z, "New task");

                    Consumer<String> run = name -> {
                        JSONObject task = taskToObject();
                        if (task.length() > 0) {
                            object.put(name, task);
                            configuration.savejson("AreaPickerTasks.json", object);
                            debugLogPing(String.format("Task %s is saved", name), Color.GREEN);
                            w.close();
                        } else {
                            debugLogPing(String.format("Task %s is empty. Not saved", name), Color.RED);
                        }
                    };

                    Consumer<String> check = name -> {
                        if (object.has(name)) {
                            Window rw = new Window(Coord.z, "Rewrite task?");
                            WidgetVerticalAppender wva = new WidgetVerticalAppender(rw);
                            wva.addRow(new Button(UI.scale(45), "Yes") {
                                @Override
                                public void click() {
                                    run.accept(name);
                                    rw.close();
                                }
                            }, new Button(UI.scale(45), "No") {
                                @Override
                                public void click() {
                                    rw.close();
                                }
                            });
                            rw.pack();
                            gui.adda(rw, gui.sz.div(2), 0.5, 0.5);
                        } else {
                            run.accept(name);
                        }
                    };
                    WidgetVerticalAppender wva = new WidgetVerticalAppender(w);
                    wva.add(new Label("Set task name:"));
                    final TextEntry value = new TextEntry(UI.scale(150), "") {
                        @Override
                        public void activate(String text) {
                            if (!text.isEmpty()) {
                                check.accept(text);
                            }
                        }
                    };
                    wva.addRow(value, new Button(UI.scale(45), "Save") {
                        @Override
                        public void click() {
                            final String text = value.text();
                            if (!text.isEmpty()) {
                                check.accept(text);
                            }
                        }
                    });
                    w.pack();
                    gui.adda(w, gui.sz.div(2), 0.5, 0.5);
                });
            }
        };
        exportbtn = new Button(UI.scale(50), "Export", () -> {
            try {
                Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                cb.setContents(new StringSelection(taskToObject().toString()), null);
                debugLogPing(String.format("Task is exported to clipboard"), Color.GREEN);
            } catch (Exception e) {
                debugLogPing(String.format("Error while exporting"), Color.GREEN);
                e.printStackTrace();
            }
        });
        exportbtn.settip("Export task to clipboard");
        importbtn = new Button(UI.scale(50), "Import", () -> {
            try {
                if (taskFromObject(new JSONObject((String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor)))) AreaPicker.this.pack();
                debugLogPing(String.format("Task is imported from clipboard"), Color.GREEN);
            } catch (Exception e) {
                debugLogPing(String.format("Error while importing"), Color.GREEN);
                e.printStackTrace();
            }
        });
        importbtn.settip("Import task from clipboard");

        scancontinuouslycheckbox = new CheckBox("") {
            @Override
            public void set(boolean a) {
                if (!isblocked()) {
                    super.set(a);
                }
            }
        };

        appender.setHorizontalMargin(5);
        appender.setVerticalMargin(2);
        appender.addRow(maininfolbl);
        appender.addRow(l1 = new Label("1. Objects to collect"), selectgobbtn, selectedgobbtn, areagobinfolbl);
        appender.addRow(l2 = new Label("2. Action"), selectedflowerbtn, flowerpetalsinfolbl);
        appender.addRow(l3 = new Label("3. Storage type"), collecttriggerdbx);
        appender.addRow(l4 = new Label("4. Objects to storage"), selectstoragebtn, selectedstoragebtn, areastorageinfolbl);
        appender.addRow(l5 = new Label("5. Items for storage"), selecteditembtn, iteminfolbl);
        appender.addRow(l6 = new Label("6. Scan continuously for 10 minutes if no objects to collect?"), scancontinuouslycheckbox);

        appender.addRow(runbtn, loadbtn, savebtn, exportbtn, importbtn);
        add(stopbtn, runbtn.c);
        add(pausumebtn, loadbtn.c);
        stopbtn.hide();
        pausumebtn.hide();

        pack();
    }

    private JSONObject taskToObject() {
        JSONObject task = new JSONObject();
        if (!selectedgoblist.isEmpty()) {
            final List<String> gobs = selectedgoblist.stream().filter(ch -> ch.selected).map(ch -> ch.name).collect(Collectors.toList());
            if (!gobs.isEmpty()) task.put("Gobs", gobs);
        }
        if (!selectedflowerlist.isEmpty()) {
            final List<String> petals = selectedflowerlist.stream().filter(ch -> ch.selected).map(ch -> ch.name).collect(Collectors.toList());
            if (!petals.isEmpty()) task.put("Flower", petals);
        }
        final int index = collecttriggerdbx.selindex;
        if (index != -1) {
            task.put("Type", index);

            if (index == 2 || index == 3) {
                if (!selectedstoragelist.isEmpty()) {
                    final List<String> storages = selectedstoragelist.stream().filter(ch -> ch.selected).map(ch -> ch.name).collect(Collectors.toList());
                    if (!storages.isEmpty()) task.put("Storages", storages);
                }

                if (!selecteditemlist.isEmpty()) {
                    final List<String> items = selecteditemlist.stream().filter(ch -> ch.selected).map(ch -> ch.name).collect(Collectors.toList());
                    if (!items.isEmpty()) task.put("Items", items);
                }
            }
        }

        if (scancontinuouslycheckbox.a) {
            task.put("ScanContinuously", scancontinuouslycheckbox.a);
        }

        return (task);
    }

    private boolean taskFromObject(JSONObject object) {
        boolean dirty = false;

        if (object.has("Gobs")) {
            final List<String> gobs = new ArrayList<>();
            Object obj = object.get("Gobs");
            if (obj instanceof JSONArray) {
                ((JSONArray) obj).forEach(i -> gobs.add(i.toString()));
            } if (obj instanceof List) {
                ((List) obj).forEach(i -> gobs.add(i.toString()));
            }

            selectedgoblbox.items.forEach(i -> i.selected = false);
            final List<String> temp = new ArrayList<>();
            final List<CheckListboxItem> outch = new ArrayList<>();
            gobs.forEach(i -> {
                CheckListboxItem ch = selectedgoblist.stream().filter(s -> s.name.equals(i)).findFirst().orElse(null);
                if (ch == null) {
                    ch = new CheckListboxItem(i);
                    selectedgoblist.add(ch);
                    outch.add(ch);
                }
                ch.selected = true;
            });
            selectedgoblist.stream().map(i -> i.name).forEach(i -> temp.add(configuration.getShortName(i) + " (" + i.substring(0, i.lastIndexOf('/')) + ")"));
            selectedgoblbox.items.addAll(outch);
            selectedgoblbox.resize(calcWidthString(temp), selectedgoblbox.sz.y);
            selectedgobwnd.pack();
            selectedgobsearch.settext("");
            if (!selectedgoblbox.items.isEmpty()) selectedgobbtn.change(Color.GREEN);
            else selectedgobbtn.change(Color.RED);

            updatelist("gob");
            updateinfo("gob");
            dirty = true;
        }

        if (object.has("Flower")) {
            final List<String> petals = new ArrayList<>();
            Object obj = object.get("Flower");
            if (obj instanceof JSONArray) {
                ((JSONArray) obj).forEach(i -> petals.add(i.toString()));
            } if (obj instanceof List) {
                ((List) obj).forEach(i -> petals.add(i.toString()));
            }

            selectedflowerlbox.items.forEach(i -> i.selected = false);
            final List<String> temp = new ArrayList<>();
            final List<CheckListboxItem> outch = new ArrayList<>();
            petals.forEach(i -> {
                CheckListboxItem ch = selectedflowerlist.stream().filter(s -> s.name.equals(i)).findFirst().orElse(null);
                if (ch == null) {
                    ch = new CheckListboxItem(i);
                    selectedflowerlist.add(ch);
                    outch.add(ch);
                }
                ch.selected = true;
            });
            selectedflowerlist.stream().map(i -> i.name).forEach(s -> {
                String loc = Resource.language.equals("en") ? s : Resource.getLocString(Resource.BUNDLE_FLOWER, s);
                temp.add(Resource.language.equals("en") ? s : loc.equals(s) ? s : s + " (" + Resource.getLocString(Resource.BUNDLE_FLOWER, s) + ")");
            });
            selectedflowerlbox.items.addAll(outch);
            selectedflowerlbox.items.sort(listboxsort());
            selectedflowerlbox.resize(calcWidthString(temp), selectedflowerlbox.sz.y);
            selectedflowerwnd.pack();
            selectedflowersearch.settext("");

            updateinfo("flower");
            dirty = true;
        }

        if (object.has("Type")) {
            final int type = object.getInt("Type");
            collecttriggerdbx.change(type);

            if (type == 2 || type == 3) {
                if (object.has("Storages")) {
                    final List<String> storages = new ArrayList<>();
                    Object obj = object.get("Storages");
                    if (obj instanceof JSONArray) {
                        ((JSONArray) obj).forEach(i -> storages.add(i.toString()));
                    } if (obj instanceof List) {
                        ((List) obj).forEach(i -> storages.add(i.toString()));
                    }

                    selectedstoragelbox.items.forEach(i -> i.selected = false);
                    final List<String> temp = new ArrayList<>();
                    final List<CheckListboxItem> outch = new ArrayList<>();
                    storages.forEach(i -> {
                        CheckListboxItem ch = selectedstoragelist.stream().filter(s -> s.name.equals(i)).findFirst().orElse(null);
                        if (ch == null) {
                            ch = new CheckListboxItem(i);
                            selectedstoragelist.add(ch);
                            outch.add(ch);
                        }
                        ch.selected = true;
                    });
                    selectedstoragelist.stream().map(i -> i.name).forEach(i -> temp.add(configuration.getShortName(i) + " (" + i.substring(0, i.lastIndexOf('/')) + ")"));
                    selectedstoragelbox.items.addAll(outch);
                    selectedstoragelbox.resize(calcWidthString(temp), selectedstoragelbox.sz.y);
                    selectedstoragewnd.pack();
                    selectedstoragesearch.settext("");
                    if (!selectedstoragelbox.items.isEmpty()) selectedstoragebtn.change(Color.GREEN);
                    else selectedstoragebtn.change(Color.RED);

                    updatelist("storage");
                    updateinfo("storage");
                    dirty = true;
                }

                if (object.has("Items")) {
                    final List<String> items = new ArrayList<>();
                    Object obj = object.get("Items");
                    if (obj instanceof JSONArray) {
                        ((JSONArray) obj).forEach(i -> items.add(i.toString()));
                    } if (obj instanceof List) {
                        ((List) obj).forEach(i -> items.add(i.toString()));
                    }

                    selecteditemlbox.items.forEach(i -> i.selected = false);
                    final List<String> temp = new ArrayList<>();
                    final List<CheckListboxItem> outch = new ArrayList<>();
                    items.forEach(i -> {
                        CheckListboxItem ch = selecteditemlist.stream().filter(s -> s.name.equals(i)).findFirst().orElse(null);
                        if (ch == null) {
                            ch = new CheckListboxItem(i);
                            selecteditemlist.add(ch);
                            outch.add(ch);
                        }
                        ch.selected = true;
                    });
                    selecteditemlist.stream().map(i -> i.name).forEach(i -> temp.add(i)); //XXX
                    selecteditemlbox.items.addAll(outch);
                    selecteditemlbox.items.sort(listboxsort());
                    selecteditemlbox.resize(calcWidthString(temp), selecteditemlbox.sz.y);
                    selecteditemwnd.pack();
                    selecteditemsearch.settext("");

                    updateinfo("item");
                    dirty = true;
                }
            }
        }

        if (object.has("ScanContinuously")) {
            scancontinuouslycheckbox.set(Boolean.parseBoolean(object.get("ScanContinuously").toString()));
        }

        return (dirty);
    }

    @Override
    public void run() {
        if (checkcollectstate() == -2 || checkcollectstate() == 3) {
            debugLogPing("Select another storage type", Color.WHITE);
            return;
        }
        runbtn.hide();
        loadbtn.hide();
        savebtn.hide();
        exportbtn.hide();
        importbtn.hide();
        stopbtn.show();
        pausumebtn.change("Pause");
        pausumebtn.show();
        paused = false;
        boolean ad = Config.autodrink;
        if (ad) Config.autodrink = false;
        boolean af = configuration.autoflower;
        if (ad) configuration.autoflower = false;
        block(true);

        collecting();

        stopbtn.hide();
        pausumebtn.hide();
        runbtn.show();
        loadbtn.show();
        savebtn.show();
        exportbtn.show();
        importbtn.show();
        if (ad) Config.autodrink = true;
        if (af) configuration.autoflower = true;
        block(false);
        maininfolbl.settext("", Color.WHITE);
    }

    public void collecting() {
        try {
            final List<PBotGob> storages = new ArrayList<>(currentstoragelist);
            final List<PBotGob> objects = new ArrayList<>(currentgoblist);
            byte cr = checkcollectstate();
            int continuousScanEmptyTries = 0;
            for (int p = 1; !objects.isEmpty() || (scancontinuouslycheckbox.a && continuousScanEmptyTries < 600); p++) {
                pauseCheck();
                addForContinuousScan(storages, objects);

                PBotGob pgob = closestGob(objects);
                if (pgob == null) {
                    objects.remove(pgob);
                    continuousScanEmptyTries++;
                    sleep(1000);
                    continue;
                }
                continuousScanEmptyTries = 0;
                for (int i = 0; ; i++) {
                    debugLog("Gob is " + p + " of " + currentgoblist.size() + ". Try is " + (i + 1), Color.YELLOW);
                    if (cr == -1 || cr == 0) {
                        if (!freeSlots() || PBotUtils.getItemAtHand(ui) != null) {
                            debugLog("Not enough space for item. Stopping...", Color.WHITE);
                            stop();
                        }
                    } else if (cr == 1) {
                        if (PBotUtils.getItemAtHand(ui) != null) {
                            debugLog("Dropping...", Color.WHITE);
                            if (!dropItemFromHand()) {
                                debugLog("Can't drop. Stopping...", Color.WHITE);
                                stop();
                            }
                        }
                    } else if (cr == 2) {
                        if (storages.isEmpty()) {
                            debugLog("Storages is full", Color.WHITE);
                            if (!freeSlots() || PBotUtils.getItemAtHand(ui) != null) {
                                debugLog("Not enough space for item. Stopping...", Color.WHITE);
                                stop();
                            }
                        }
                        if (PBotUtils.getItemAtHand(ui) != null) {
                            debugLog("Dropping...", Color.WHITE);
                            if (!dropItemFromHand()) {
                                debugLog("Can't drop. Stopping...", Color.WHITE);
                                stop();
                            }
                        }
                        PBotItem bigitem = getMaxSizeItem(getInvItems(selecteditemlist));
                        if (bigitem != null && PBotUtils.playerInventory(ui).freeSpaceForItem(bigitem) == null) {
                            if (PBotUtils.getItemAtHand(ui) != null) PBotUtils.dropItemFromHand(ui, 0, 1000);
                            storaging(storages);
                            break;
                        }
                    }
                    if (PBotGobAPI.findGobById(ui, pgob.getGobId()) == null) {
                        debugLog("Object not found. Skipping...", Color.WHITE);
                        objects.remove(pgob);
                        break;
                    }

                    if (!checkFlowerMenu(pgob)) {
                        objects.remove(pgob);
                        break;
                    }
                    mark(pgob);
                    //action
                    List<String> flowers = checkflowers();
                    List<String> actions = containsActions(flowers);
                    task:
                    {
                        if (!actions.isEmpty()) {
                            if (pfLeftClick(pgob)) {
                                String actionItem = actions.get(0);
                                Matcher matcher = ACTION_PATTERN.matcher(actionItem);
                                if (matcher.find()) {
                                    String action = matcher.group(1);
                                    String value = null;

                                    if (actions1.containsKey(action)) {
                                        value = actions1.get(action);
                                    } else if (actions2.containsKey(action)) {
                                        value = actions2.get(action);
                                    }

                                    if (value != null) {
                                        PBotCharacterAPI.doAct(ui, value);
                                        pgob.doClick(1, 0);
                                        waitMoving();
                                        PBotCharacterAPI.cancelAct(ui);

                                        if (actions2.containsKey(action)) {
                                            byte wr = waitForHourglass();
                                            if (wr == 1)
                                                debugLog("hourglass is finish", Color.WHITE);
                                            else if (wr == 0)
                                                debugLog("hourglass timeout", Color.WHITE);
                                            else if (wr == 2) {
                                                debugLog("hourglass stopped. folding", Color.WHITE);
                                            }

                                            break task;
                                        }
                                    }
                                }
                            }
                        } else {
                            if (pfRightClick(pgob)) {
//                        List<String> flowers = checkflowers();
                                if (flowers.isEmpty()) {
                                    debugLog("flowermenu not required", Color.WHITE);
                                    if (waitForPickUp(pgob.getGobId())) {
                                        //remove
                                    } else {
                                        break task;
                                    }
                                } else {
                                    waitForFlowerMenu();
                                    if (petalExists()) {
                                        if (choosePetal()) {
                                            if (!waitFlowermenuClose()) {
                                                debugLog("Can't close the flowermenu", Color.WHITE);
                                                stop();
                                            }
                                            waitMoving();
                                            byte wr = waitForHourglass();
                                            if (wr == 1)
                                                debugLog("hourglass is finish", Color.WHITE);
                                            else if (wr == 0)
                                                debugLog("hourglass timeout", Color.WHITE);
                                            else if (wr == 2) {
                                                debugLog("hourglass stopped. folding", Color.WHITE);
                                                storaging(storages);
                                            }
                                            break task;
                                        } else {
                                            if (!closeFlowermenu()) {
                                                debugLog("Can't close the flowermenu", Color.WHITE);
                                                stop();
                                                break task;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        {
                            sleep(250);
                            objects.remove(pgob);
                            break;
                        }
                    }
                    sleep(250);
                }
            }

            if (cr == 2) {
                if (storages.isEmpty()) {
                    debugLog("Storages is full", Color.WHITE);
                    if (!freeSlots() || PBotUtils.getItemAtHand(ui) != null) {
                        debugLog("Not enough space for item. Stopping...", Color.WHITE);
                        stop();
                    }
                }
                if (PBotUtils.getItemAtHand(ui) != null) {
                    debugLog("Dropping...", Color.WHITE);
                    if (!dropItemFromHand()) {
                        debugLog("Can't drop. Stopping...", Color.WHITE);
                        stop();
                    }
                }
                if (getInvItems(selecteditemlist).isEmpty()) {
                    debugLog("Inventory is empty", Color.WHITE);
                    stop();
                }
                storaging(storages);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception other) {
            other.printStackTrace();
            try {
                stop();
            } catch (InterruptedException ignore) {
            }
        }
        debugLogPing("Finish!", Color.GREEN);
    }

    private void addForContinuousScan(List<PBotGob> storages, List<PBotGob> objects) {
        if (scancontinuouslycheckbox.a) {
            List<PBotGob> newStorageGobs = findGobsInArea(storagearea, selectedstoragelist, currentstoragelist);
            currentstoragelist.addAll(newStorageGobs);
            storages.addAll(newStorageGobs);

            List<PBotGob> newGobs = findGobsInArea(gobarea, selectedgoblist, currentgoblist);
            currentgoblist.addAll(newGobs);
            objects.addAll(newGobs);
        }
    }

    public void storaging(final List<PBotGob> storages) throws InterruptedException {
        final List<PBotGob> output = new ArrayList<>(storages);
        Runnable finish = () -> {
            storages.clear();
            storages.addAll(output);
        };
        for (int p = 0; p < storages.size(); p++) {
            for (int i = 0; ; i++) {
                pauseCheck();
                debugLog("Storage is " + (p + 1) + " of " + storages.size() + ". Try is " + (i + 1), Color.YELLOW);
                if (PBotGobAPI.findGobById(ui, storages.get(p).getGobId()) == null) {
                    debugLog("Object not found. Skipping...", Color.WHITE);
                    break;
                }
                mark(storages.get(p));
                final List<Window> ow = invWindows();
                if (pfRightClick(storages.get(p))) {
                    if (PBotUtils.getItemAtHand(ui) != null) PBotUtils.dropItemFromHand(ui, 0, 1000);
                    Window w = waitForNewInvWindow(ow);
                    if (w != null) {
                        PBotInventory wi = PBotWindowAPI.getInventory(w);
                        ISBox isBox = w.getchild(ISBox.class);
                        if (wi != null) {
                            if (wi.freeSlotsInv() > 0) {
                                final List<PBotItem> items = getInvItems(selecteditemlist);
                                for (PBotItem pitem : items) {
                                    if (!PBotUtils.playerInventory(ui).inv.containschild(pitem.gitem)) continue;
                                    if (wi.freeSlotsInv() == 0) {
                                        break;
                                    }
                                    if (wi.freeSpaceForItem(pitem) != null) {
                                        int invitems = wi.inv.getchilds(GItem.class).size();
                                        pitem.transferItem();
                                        while (wi.inv.getchilds(GItem.class).size() == invitems) {
                                            sleep(25);
                                        }
                                    }
                                }
                                if (getInvItems(selecteditemlist).isEmpty()) {
                                    PBotWindowAPI.closeWindow(w);
                                    waitForWindowClose(w);
                                    finish.run();
                                    return;
                                }
                            }
                            if (wi.freeSlotsInv() == 0) {
                                PBotWindowAPI.closeWindow(w);
                                waitForWindowClose(w);
                                output.remove(storages.get(p));
                                break;
                            }
                        } else if (isBox != null) {
                            if (isBox.getfreespace() > 0) {
                                final List<PBotItem> items = getInvItems(selecteditemlist);
                                for (PBotItem pitem : items) {
                                    if (!PBotUtils.playerInventory(ui).inv.containschild(pitem.gitem)) continue;
                                    if (isBox.getfreespace() == 0) {
                                        break;
                                    }
                                    {
                                        int invitems = isBox.getUsedCapacity();
                                        pitem.transferItem();
                                        while (invitems == isBox.getUsedCapacity()) {
                                            sleep(25);
                                        }
                                    }
                                }
                                if (getInvItems(selecteditemlist).isEmpty()) {
                                    PBotWindowAPI.closeWindow(w);
                                    waitForWindowClose(w);
                                    finish.run();
                                    return;
                                }
                            }
                            if (isBox.getfreespace() == 0) {
                                PBotWindowAPI.closeWindow(w);
                                waitForWindowClose(w);
                                output.remove(storages.get(p));
                                break;
                            }
                        }
                    } else
                        break;
                } else
                    break;
                sleep(1);
            }
        }
        finish.run();
    }


    public boolean pfRightClick(PBotGob pgob) throws InterruptedException {
        debugLog("pathfinding " + pgob + "...", Color.WHITE);
        for (int i = 0; i < retry; i++) {
            pauseCheck();
            debugLog("try " + (i + 1) + " of " + retry + " pathfinding " + pgob + "...", Color.WHITE);
            boolean yea = cheakHit(pgob);

            //1. purus check the path
            if (!yea) {
                debugLog("purus path", Color.WHITE);
                ui.gui.map.purusPfRightClick(pgob.gob, -1, 1, 0, null);

                while (ui.gui.map.pastaPathfinder.isAlive() && !ui.gui.map.pastaPathfinder.isInterrupted())
                    sleep(10);

                yea = ui.gui.map.foundPath;

                debugLog("purus path" + (yea ? "" : " not") + " found", yea ? Color.GREEN : Color.RED);
            }

            //2. sloth check the path
            if (!yea) {
                debugLog("sloth path", Color.WHITE);
                yea = ui.gui.map.pathto(pgob.gob);
                for (int t = 0, sleep = 10; !ui.gui.map.isclearmovequeue(); t += sleep) {
                    sleep(10);
                }

                debugLog("sloth path" + (yea ? "" : " not ") + " found", yea ? Color.GREEN : Color.RED);
            }

            //3. amber check the path
            if (!yea) {
                debugLog("amber path", Color.WHITE);
                ui.gui.map.pfRightClick(pgob.gob, -1, 1, 0, null);
                while (!ui.gui.map.pfthread.isInterrupted() && ui.gui.map.pfthread.isAlive())
                    sleep(10);

                yea = cheakHit(pgob);
            }

            //4. without pf
            if (!yea) {
                debugLog("hard path", Color.WHITE);
                Coord2d opc = PBotGobAPI.player(ui).getRcCoords();
                pgob.doClick(1, 0);
                waitMoving();
                Coord2d npc = PBotGobAPI.player(ui).getRcCoords();
                Coord2d dist = npc.div(opc);
                double x = dist.x > 0 ? 1 : -1;
                double y = dist.y > 0 ? 1 : -1;
                PBotUtils.mapClick(ui, PBotGobAPI.player(ui).getRcCoords().add(x, y), 1, 0);
                waitMoving();

                yea = cheakHit(pgob);
            }

            sleep(1);

            if (yea) {
                debugLog("path found", Color.GREEN);
                pgob.doClick(3, 0);
                waitMoving();
                return (true);
            } else
                debugLog("path not found", Color.RED);
        }
        return (false);
    }

    public boolean pfLeftClick(PBotGob pgob) throws InterruptedException {
        debugLog("pathfinding " + pgob + "...", Color.WHITE);
        for (int i = 0; i < retry; i++) {
            pauseCheck();
            debugLog("try " + (i + 1) + " of " + retry + " pathfinding " + pgob + "...", Color.WHITE);
            boolean yea = cheakHit(pgob);

            //1. purus check the path
            if (!yea) {
                debugLog("purus path", Color.WHITE);
                ui.gui.map.purusPfRightClick(pgob.gob, -1, 1, 0, null);

                while (ui.gui.map.pastaPathfinder.isAlive() && !ui.gui.map.pastaPathfinder.isInterrupted())
                    sleep(10);

                yea = ui.gui.map.foundPath;

                debugLog("purus path" + (yea ? "" : " not") + " found", yea ? Color.GREEN : Color.RED);
            }

            //2. sloth check the path
            if (!yea) {
                debugLog("sloth path", Color.WHITE);
                yea = ui.gui.map.pathto(pgob.gob);
                for (int t = 0, sleep = 10; !ui.gui.map.isclearmovequeue(); t += sleep) {
                    sleep(10);
                }

                debugLog("sloth path" + (yea ? "" : " not ") + " found", yea ? Color.GREEN : Color.RED);
            }

            //3. amber check the path
            if (!yea) {
                debugLog("amber path", Color.WHITE);
                ui.gui.map.pfRightClick(pgob.gob, -1, 1, 0, null);
                while (!ui.gui.map.pfthread.isInterrupted() && ui.gui.map.pfthread.isAlive())
                    sleep(10);

                yea = cheakHit(pgob);
            }

            //4. without pf
            if (!yea) {
                debugLog("hard path", Color.WHITE);
                Coord2d opc = PBotGobAPI.player(ui).getRcCoords();
                pgob.doClick(1, 0);
                waitMoving();
                Coord2d npc = PBotGobAPI.player(ui).getRcCoords();
                Coord2d dist = npc.div(opc);
                double x = dist.x > 0 ? 1 : -1;
                double y = dist.y > 0 ? 1 : -1;
                PBotUtils.mapClick(ui, PBotGobAPI.player(ui).getRcCoords().add(x, y), 1, 0);
                waitMoving();

                yea = cheakHit(pgob);
            }

            sleep(1);

            if (yea) {
                debugLog("path found", Color.GREEN);
//                pgob.doClick(3, 0);
                waitMoving();
                return (true);
            } else
                debugLog("path not found", Color.RED);
        }
        return (false);
    }

    public boolean cheakHit(PBotGob pgob) {
        PBotGob player = PBotGobAPI.player(ui);
        Hitbox[] box = Hitbox.hbfor(pgob.gob);
        Hitbox[] pbox = Hitbox.hbfor(player.gob);
        if (box != null && pbox != null) {
            boolean hit = false;
            for (Hitbox hb1 : box)
                for (Hitbox hb2 : pbox) {
                    if (hb1.ishitable()) {
                        hit = true;
                        if (configuration.insect(hb1.points, configuration.abs(hb2.points, 1), pgob.gob, player.gob))
                            return (true);
                    }
                }
            if (!hit) {
                return (pgob.getRcCoords().dist(player.getRcCoords()) <= 3);
            }
            return (false);
        } else
            return (pgob.getRcCoords().dist(player.getRcCoords()) <= 3);
    }

    public void waitMoving() throws InterruptedException {
        debugLog("moving...", Color.WHITE);
        while (PBotGobAPI.player(ui).isMoving()) {
            sleep(10);
        }
        debugLog("move stop", Color.WHITE);
    }

    public void mark(PBotGob pgob) {
        pgob.gob.mark(5000);
    }


    public boolean checkFlowerMenu(PBotGob pgob) throws InterruptedException {
        final List<String> temp = new ArrayList<>();
        for (CheckListboxItem item : selectedflowerlist)
            if (item.selected)
                temp.add(item.name);
        if (temp.isEmpty()) return (true);
        if (!containsActions(temp).isEmpty()) return (true);

        pgob.doClick(3, 0);
        waitForFlowerMenu();

        boolean found = false;
        if (petalExists()) {
            FlowerMenu menu = ui.root.getchild(FlowerMenu.class);
            flower:
            for (int i = 0; i < menu.opts.length; i++) {
                for (String item : temp) {
                    if (item.equals(menu.opts[i].name)) {
                        found = true;
                        break flower;
                    }
                }
            }
            if (!closeFlowermenu()) {
                debugLog("Can't close the flowermenu", Color.WHITE);
                stop();
            }
        }
        return (found);
    }

    public boolean choosePetal() throws InterruptedException {
        debugLog("petal choosing...", Color.WHITE);
        final List<String> temp = new ArrayList<>();
        for (CheckListboxItem item : selectedflowerlist)
            if (item.selected)
                temp.add(item.name);
        for (int i = 0, sleep = 10; i < waitingtime; i += sleep) {
            FlowerMenu menu = ui.root.getchild(FlowerMenu.class);
            if (menu != null) {
                for (FlowerMenu.Petal opt : menu.opts) {
                    for (String item : temp) {
                        if (opt.name.equals(item)) {
                            debugLog("choosePetal [" + item + "] true", Color.GREEN);
                            menu.choose(opt);
                            menu.destroy();
                            return (true);
                        }
                    }
                }
            }
            sleep(sleep);
        }
        debugLog("choosePetals " + temp.toString() + " false", Color.RED);
        return (false);
    }

    public boolean waitFlowermenuClose() {
        debugLog("flowermenu closing waiting...", Color.WHITE);
        for (int i = 0; i < retry; i++)
            if (PBotUtils.waitFlowermenuClose(ui, waitingtime)) {
                debugLog("flowermenu closed", Color.WHITE);
                return (true);
            } else
                debugLog("flowermenu didn't close", Color.WHITE);
        return (false);
    }

    public boolean waitForPickUp(long id) throws InterruptedException {
        debugLog("pick up ground item waiting...", Color.WHITE);
        boolean r = false;
        for (int i = 0, sleep = 10; i < waitingtime; i += sleep) {
            if (PBotGobAPI.findGobById(ui, id) == null) {
                r = true;
                break;
            }
            sleep(sleep);
        }

        if (r)
            debugLog("ground item picked", Color.WHITE);
        else
            debugLog("ground item didn't pick", Color.WHITE);
        return (r);
    }

    public boolean waitForFlowerMenu() {
        debugLog("flowermenu opening waiting...", Color.WHITE);
        boolean r = PBotUtils.waitForFlowerMenu(ui, waitingtime);
        if (r)
            debugLog("flowermenu opened", Color.WHITE);
        else
            debugLog("flowermenu didn't open", Color.WHITE);
        return (r);
    }

    public boolean petalExists() {
        debugLog("petal checking...", Color.WHITE);
        boolean r = PBotUtils.petalExists(ui);
        if (r)
            debugLog("petal found", Color.WHITE);
        else
            debugLog("petal not found", Color.WHITE);
        return (r);
    }

    public boolean closeFlowermenu() {
        debugLog("flowermenu closing...", Color.WHITE);
        for (int i = 0; i < retry; i++)
            if (PBotUtils.closeFlowermenu(ui, waitingtime)) {
                debugLog("flowermenu closed", Color.WHITE);
                return (true);
            } else
                debugLog("flowermenu didn't close", Color.WHITE);
        return (false);
    }


    public boolean drink() throws InterruptedException {
        if (!ui.gui.drinkingWater) {
            Thread t = new Thread(new DrinkWater(ui.gui));
            t.start();
            while (t.isAlive() && !t.isInterrupted()) {
                sleep(10);
            }
            if (!ui.gui.lastDrinkingSucessful) {
                debugLog("PBotUtils Warning: Couldn't drink, didn't find anything to drink!", Color.ORANGE);
                return (false);
            }
        }
        return (true);
    }


    public byte waitForHourglass() throws InterruptedException {
        boolean repeat = true;
        while (repeat) {
            repeat = false;
            debugLog("hourglass waiting...", Color.WHITE);
            double prog = ui.gui.prog;
            for (int i = 0, sleep = 5; prog == -1; i += sleep) {
                if (i > waitingtime)
                    return (0);
                prog = ui.gui.prog;
                sleep(sleep);
            }
            int total = 0;
            for (int sleep = 5; ui.gui.prog >= 0; total += sleep) {
                pauseCheck();
                if (PBotCharacterAPI.getStamina(ui) < 70) {//FIXME custom value
                    debugLog("Drinking...", Color.WHITE);
                    if (!drink()) {
                        debugLog("Drink failed. Pause!", Color.WHITE);
                        pause();
                    } else {
                        debugLog("Drink successful", Color.WHITE);
                        repeat = true;
                        break;
                    }
                }

                byte cr = checkcollectstate();
                if (cr == -1 || cr == 0) {
                    if (!freeSlots() || PBotUtils.getItemAtHand(ui) != null) {
                        debugLog("Not enough space for item. Stopping...", Color.WHITE);
                        stop();
                        return (-1);
                    }
                }
                if (cr == 1) {
                    if (PBotUtils.getItemAtHand(ui) != null) {
                        debugLog("Dropping...", Color.WHITE);
                        if (!dropItemFromHand()) {
                            debugLog("Can't drop. Stopping...", Color.WHITE);
                            stop();
                            return (-1);
                        }
                    }
                }
                if (cr == 2) {
                    if (PBotUtils.getItemAtHand(ui) != null) {
                        debugLog("Dropping...", Color.WHITE);
                        if (!dropItemFromHand()) {
                            debugLog("Can't drop. Stopping...", Color.WHITE);
                            stop();
                            return (-1);
                        }
                    }
                    PBotItem bigitem = getMaxSizeItem(getInvItems(selecteditemlist));
                    if (bigitem != null && PBotUtils.playerInventory(ui).freeSpaceForItem(bigitem) == null) {
                        debugLog("Not enough space for item. Folding...", Color.WHITE);
                        ui.root.wdgmsg("gk", 27);
                        return (2);
                    }
                }
                sleep(sleep);
            }
            debugLog("hourglass finish at " + total + " ms", Color.WHITE);
        }
        return (1);
    }


    public Window waitForNewInvWindow(final List<Window> ows) throws InterruptedException {//FIXME stockpile and barrel
        debugLog("inventory window opening waiting...", Color.WHITE);
        for (int i = 0, sleep = 10; i < waitingtime; i += sleep) {
            for (Window iw : invWindows()) {
                if (ows.stream().noneMatch(iw::equals)) {
                    debugLog("inventory window opened", Color.WHITE);
                    return (iw);
                }
            }
            sleep(sleep);
        }

        debugLog("inventory window didn't open", Color.WHITE);
        return (null);
    }

    public boolean waitForWindowClose(Window w) throws InterruptedException {
        debugLog("window closing waiting...", Color.WHITE);
        for (int i = 0, sleep = 50; i < waitingtime && ui.gui.getwnd(w) != null; i += sleep) {
            sleep(sleep);
        }
        if (ui.gui.getwnd(w) == null) {
            debugLog("window closed", Color.WHITE);
            return (true);
        } else {
            debugLog("window didn't close", Color.WHITE);
            return (false);
        }
    }

    public List<Window> invWindows() {
        final List<Window> iwnds = new ArrayList<>();
        for (Window w : ui.gui.getchilds(Window.class)) {
            if (w.getchild(Inventory.class) != null || w.getchild(ISBox.class) != null)
                iwnds.add(w);
        }
        return (iwnds);
    }


    public void debugLog(String msg, Color clr) {
        try {
            maininfolbl.settext(msg, clr);
            System.out.println("AreaPicker: " + msg);
            ui.gui.debuglog.append(msg, clr);
        } catch (Exception ignore) {}
    }

    public void debugLogPing(String msg, Color clr) {
        try {
            ui.gui.debugmsg(msg, clr);
        } catch (Exception ignore) {}
    }


    public void selectArea() {
        debugLogPing("Please select an area by dragging!", Color.WHITE);
        ui.gui.map.PBotAPISelect = true;
        while (ui.gui.map.PBotAPISelect) {
            PBotUtils.sleep(25);
        }
    }

    public class selectinggobarea implements Runnable {
        @Override
        public void run() {
            selectArea();
            gobarea = new Area(PBotUtils.getSelectedAreaA(), PBotUtils.getSelectedAreaB());

            areagoblist.clear();
            areagoblist.addAll(stringInArea(gobarea));

//            selectedgoblist.clear();
//            selectedgoblbox.items.clear();
            final List<String> out = selectedgoblist.stream().map(i -> i.name).collect(Collectors.toList());
            final List<String> temp = new ArrayList<>();
            final List<CheckListboxItem> outch = new ArrayList<>();
            areagoblist.forEach(i -> {
                if (!out.contains(i)) {
                    CheckListboxItem ch = new CheckListboxItem(i);
                    selectedgoblist.add(ch);
                    outch.add(ch);
                }
            });
            selectedgoblist.stream().map(i -> i.name).forEach(i -> temp.add(configuration.getShortName(i) + " (" + i.substring(0, i.lastIndexOf('/')) + ")"));
            selectedgoblbox.items.addAll(outch);
            selectedgoblbox.resize(calcWidthString(temp), selectedgoblbox.sz.y);
            selectedgobwnd.pack();
            selectedgobsearch.settext("");
            if (!selectedgoblbox.items.isEmpty()) selectedgobbtn.change(Color.GREEN);
            else selectedgobbtn.change(Color.RED);

            selectgobbtn.change(Color.GREEN);

            updatelist("gob");
            updateinfo("gob");
            pack();
        }
    }

    public class selectingstoragearea implements Runnable {
        @Override
        public void run() {
            selectArea();
            storagearea = new Area(PBotUtils.getSelectedAreaA(), PBotUtils.getSelectedAreaB());

            areastoragelist.clear();
            areastoragelist.addAll(stringInArea(storagearea));

//            selectedstoragelist.clear();
//            selectedstoragelbox.items.clear();
            final List<String> out = selectedstoragelist.stream().map(i -> i.name).collect(Collectors.toList());
            final List<String> temp = new ArrayList<>();
            final List<CheckListboxItem> outch = new ArrayList<>();
            areastoragelist.forEach((i) -> {
                if (!out.contains(i)) {
                    CheckListboxItem ch = new CheckListboxItem(i);
                    selectedstoragelist.add(ch);
                    outch.add(ch);
                }
            });
            selectedstoragelist.stream().map(i -> i.name).forEach(i -> temp.add(configuration.getShortName(i) + " (" + i.substring(0, i.lastIndexOf('/')) + ")"));
            selectedstoragelbox.items.addAll(outch);
            selectedstoragelbox.resize(calcWidthString(temp), selectedstoragelbox.sz.y);
            selectedstoragewnd.pack();
            selectedstoragesearch.settext("");
            if (!selectedstoragelbox.items.isEmpty()) selectedstoragebtn.change(Color.GREEN);
            else selectedstoragebtn.change(Color.RED);

            selectstoragebtn.change(Color.GREEN);

            updatelist("storage");
            updateinfo("storage");
            pack();
        }
    }

    public void sleep(long time) throws InterruptedException {
        Thread.sleep(time);
    }


    public boolean freeSlots() throws InterruptedException {
//        debugLog("free slots checking...", Color.WHITE);
        boolean free = false;
        int slots = -1;
        while (slots == -1) {
            Inventory inv = PBotUtils.playerInventory(ui).inv;

            int takenSlots = 0;
            for (Widget i = inv.child; i != null; i = i.next) {
                if (i instanceof WItem) {
                    WItem buf = (WItem) i;
                    int s = 0;
                    for (int t = 0, sleep = 10; s == 0 && t < waitingtime; t += sleep) {
                        s = buf.size().x * buf.size().y;
                        sleep(sleep);
                    }
                    takenSlots += s;
                }
            }
            int allSlots = inv.getMaxSlots();
            slots = allSlots - takenSlots;

            if (slots > 0) free = true;
//            debugLog("free slots checked " + slots, Color.WHITE);
        }
//        debugLog("free slots " + free, Color.WHITE);
        return free;
    }

    public boolean dropItemFromHand() {
        debugLog("dropping...", Color.WHITE);
        for (int i = 0; i < retry; i++) {
            if (PBotUtils.dropItemFromHand(ui, 0, 200)) {
                debugLog("dropped", Color.WHITE);
                return (true);
            } else
                debugLog("dropping failed", Color.WHITE);
        }
        return (false);
    }

    public List<PBotItem> getInvItems(final List<CheckListboxItem> clist) {
        final List<String> list = new ArrayList<>();
        for (CheckListboxItem i : clist) {
            if (i.selected)
                list.add(i.name);
        }
        return PBotUtils.playerInventory(ui).getInventoryContainsResnames(list);
    }

    public PBotItem getMaxSizeItem(final List<PBotItem> list) {
        PBotItem big = null;
        for (PBotItem i : list) {
            if (i != null) {
                if (big == null)
                    big = i;
                else {
                    Coord bs = big.witem.size();
                    Coord is = i.witem.size();
                    if (bs.x < is.x || bs.y < is.y)
                        big = i;
                }
            }
        }
        return big;
    }


    public byte checkcollectstate() {
        if (collecttriggerdbx.sel == null) return (-1);
        for (int i = 0; i < collectstates.length; i++) {
            if (collecttriggerdbx.sel.equals(collectstates[i])) return ((byte) i);
        }
        return (-2);
    }

    public List<String> checkflowers() {
        final List<String> temp = new ArrayList<>();
        for (CheckListboxItem item : selectedflowerlist) {
            if (item.selected)
                temp.add(item.name);
        }
        return (temp);
    }

    public List<String> containsActions(List<String> flowers) {
        final List<String> temp = new ArrayList<>();
        for (String flower : flowers) {
            if (flower.startsWith(ACTION_START))
                temp.add(flower);
        }
        return (temp);
    }

    public List<String> checkitems() {
        final List<String> temp = new ArrayList<>();
        for (CheckListboxItem item : selecteditemlist) {
            if (item.selected)
                temp.add(item.name);
        }
        return (temp);
    }

    public List<String> stringInArea(final Area a) {
        final List<PBotGob> gobs = PBotUtils.gobsInArea(ui, a.ul, a.br);
        final List<String> strings = new ArrayList<>();

        for (PBotGob pgob : gobs) {
            if (pgob.getResname() != null && !strings.contains(pgob.getResname()))
                strings.add(pgob.getResname());
        }

        return strings;
    }

    public List<String> stringInInvContent() {
        final List<String> strings = new ArrayList<>();
        final List<PBotInventory> ret = new ArrayList<>();
        for (Widget window = ui.gui.lchild; window != null; window = window.prev)
            if (window instanceof Window && !((Window) window).origcap.equalsIgnoreCase("Belt"))
                for (Widget wdg = window.lchild; wdg != null; wdg = wdg.prev)
                    if (wdg instanceof Inventory)
                        ret.add(new PBotInventory((Inventory) wdg));
        for (PBotInventory inv : ret) {
            final List<PBotItem> items = inv.getInventoryContents();
            for (PBotItem ptem : items) {
                if (ptem.getResname() != null && !strings.contains(ptem.getResname()))
                    strings.add(ptem.getResname());
            }
        }
        return strings;
    }

    public void updateitemlist() {
        final List<String> items = stringInInvContent();
        for (String s : items) {
            boolean contains = false;
            for (CheckListboxItem i : selecteditemlist) {
                if (s.equalsIgnoreCase(i.name)) {
                    contains = true;
                    break;
                }
            }
            if (!contains)
                selecteditemlist.add(new CheckListboxItem(s));
        }
        for (String s : addeditemlist) {
            boolean contains = false;
            for (CheckListboxItem i : selecteditemlist) {
                if (s.equalsIgnoreCase(i.name)) {
                    contains = true;
                    break;
                }
            }
            if (!contains)
                selecteditemlist.add(new CheckListboxItem(s));
        }
    }

    public void areainfo(Label lbl, final Area area, final List<CheckListboxItem> checklist, int gobCount) {
        if (area != null) {
            StringBuilder sb = new StringBuilder();
            int x = Math.abs(area.br.x - area.ul.x) / 11;
            int y = Math.abs(area.br.y - area.ul.y) / 11;
            sb.append("Area ").append(x).append("x").append(y);
            if (!checklist.isEmpty()) {
                sb.append(" : ");
                sb.append(gobCount).append(" selected objects");
            }
            if (lbl != null) {
                lbl.settext(sb.toString());
                pack();
            }
        }
    }

    public void updateinfo(String type) {
        switch (type) {
            case "gob":
                areainfo(areagobinfolbl, gobarea, selectedgoblist, currentgoblist.size());
//                pack();
                break;
            case "storage":
                areainfo(areastorageinfolbl, storagearea, selectedstoragelist, currentstoragelist.size());
//                pack();
                break;
            case "flower":
                flowerpetalsinfolbl.settext(!checkflowers().isEmpty() ? checkflowers().toString() : "[Right Click]");
                pack();
                break;
            case "item":
                iteminfolbl.settext(checkitems().toString());
                pack();
                break;
        }
    }

    public void updatelist(String type) {
        if (type.equals("gob")) {
            currentgoblist.clear();
            if (gobarea != null) {
                currentgoblist.addAll(findGobsInArea(gobarea, selectedgoblist, currentgoblist));
            }
        } else if (type.equals("storage")) {
            currentstoragelist.clear();
            if (storagearea != null) {
                currentstoragelist.addAll(findGobsInArea(storagearea, selectedstoragelist, currentstoragelist));
            }
        }
    }

    private List<PBotGob> findGobsInArea(Area area, List<CheckListboxItem> gobResnames, List<PBotGob> excludedGobs) {
        if (area == null) {
            return Collections.emptyList();
        }

        List<PBotGob> gobs = PBotUtils.gobsInArea(ui, area.ul, area.br);
        Set<String> selectedItemResnames = gobResnames.stream()
                .filter(i -> i.selected && i.name != null)
                .map(i -> i.name)
                .collect(Collectors.toSet());
        Set<Long> excludedGobIds = excludedGobs.stream()
                .map(g -> g.getGobId())
                .collect(Collectors.toSet());

        return gobs.stream()
                .filter(g -> selectedItemResnames.contains(g.getResname()))
                .filter(g -> !excludedGobIds.contains(g.getGobId()))
                .collect(Collectors.toList());
    }

    public Coord calcDropboxSize(final List<String> list) {
        Optional<Integer> ow = list.stream().map((v) -> Text.render(v).sz().x).collect(Collectors.toList()).stream().reduce(Integer::max);
        int w = Dropbox.drop.sz().x + UI.scale(5);
        if (ow.isPresent() && !list.isEmpty())
            w += ow.get();
        int h = Math.max(!list.isEmpty() ? Text.render(list.get(0)).sz().y : 0, UI.scale(16));
        return new Coord(w, h);
    }

    public int calcWidthCheckListbox(final List<CheckListboxItem> list) {
        Optional<Integer> ow = list.stream().map((v) -> Text.render(v.name).sz().x).collect(Collectors.toList()).stream().reduce(Integer::max);
        int w = Scrollbar.sflarp.sz().x + CheckListbox.chk.sz().x + UI.scale(5);
        if (ow.isPresent() && !list.isEmpty())
            w += ow.get();
        return (w);
    }

    public int calcWidthString(final List<String> list) {
        Optional<Integer> ow = list.stream().map((v) -> Text.render(v).sz().x).collect(Collectors.toList()).stream().reduce(Integer::max);
        int w = Scrollbar.sflarp.sz().x + CheckListbox.chk.sz().x + UI.scale(5);
        if (ow.isPresent() && !list.isEmpty())
            w += ow.get();
        return (w);
    }

    public Comparator<CheckListboxItem> listboxsort() {
        return (o1, o2) -> {
            int b = Boolean.compare(o2.selected, o1.selected);
            return b == 0 ? configuration.getShortName(o1.name).compareTo(configuration.getShortName(o2.name)) : b;
        };
    }

    public PBotGob closestGob(final List<PBotGob> list) {
        double min = Double.MAX_VALUE;
        PBotGob pgob = null;
        Coord2d pc = PBotGobAPI.player(ui).getRcCoords();
        for (PBotGob g : list) {
            if (pc.dist(g.getRcCoords()) < min) {
                min = Math.min(min, pc.dist(g.getRcCoords()));
                pgob = g;
            }
        }
        return (pgob);
    }

    public void stop() throws InterruptedException {
        if (runthread != null && runthread.isAlive())
            runthread.interrupt();
        ui.root.wdgmsg("gk", 27);
        sleep(1);
    }

    public void pause() {
        synchronized (pauseLock) {
            paused = true;
            pausumebtn.change("Resume");
        }
    }

    public void pauseCheck() throws InterruptedException {
        synchronized (pauseLock) {
            if (paused) {
                pauseLock.wait(1000);
            }
        }
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
            pausumebtn.change("Pause");
        }
    }

    public void block(boolean state) {
        block = state;
    }

    public boolean isblocked() {
        return block;
    }

    @Override
    public void draw(GOut g) {
        if (collecttriggerdbx != null && l4 != null && selectstoragebtn != null && selectedstoragebtn != null && areastorageinfolbl != null && l5 != null && selecteditembtn != null && iteminfolbl != null) {
            byte cr = checkcollectstate();
            if (cr == -1 || cr == 0 || cr == 1) {
                if (l4.visible) l4.hide();
                if (selectstoragebtn.visible) selectstoragebtn.hide();
                if (selectedstoragebtn.visible) selectedstoragebtn.hide();
                if (areastorageinfolbl.visible) areastorageinfolbl.hide();
                if (l5.visible) l5.hide();
                if (selecteditembtn.visible) selecteditembtn.hide();
                if (iteminfolbl.visible) iteminfolbl.hide();
            } else if (cr == 2 || cr == 3) {
                if (!l4.visible) l4.show();
                if (!selectstoragebtn.visible) selectstoragebtn.show();
                if (!selectedstoragebtn.visible) selectedstoragebtn.show();
                if (!areastorageinfolbl.visible) areastorageinfolbl.show();
                if (!l5.visible) l5.show();
                if (!selecteditembtn.visible) selecteditembtn.show();
                if (!iteminfolbl.visible) iteminfolbl.show();
            }
        }
        super.draw(g);
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            stop();
        } catch (InterruptedException ignore) {
        }
    }


//    public Dropbox<String> dropbox(final List<String> list, String type) {
//        Coord size = calcDropboxSize(list);
//        return new Dropbox<String>(size.x, 10, size.y) {
//            protected String listitem(int i) {
//                return list.get(i);
//            }
//
//            protected int listitems() {
//                return list.size();
//            }
//
//            protected void drawitem(GOut g, String item, int i) {
//                g.text(item, Coord.z);
//            }
//
//            public void change(String item) {
//                super.change(item);
//                resize(Text.render(item).sz().x + drop.sz().x + 2, calcDropboxSize(list).y);
//                updatelist(type);
//                updateinfo(type);
//            }
//
//            public boolean mousedown(Coord c, int btn) {
//                if (!isblocked()) {
//                    super.mousedown(c, btn);
//                    if (dl != null) resizedl(list);
//                }
//                return (true);
//            }
//        };
//    }
}
