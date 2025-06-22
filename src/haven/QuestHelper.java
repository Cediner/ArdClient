package haven;

import haven.chrwnd.QuestWnd;
import haven.sloth.gui.ResizableWnd;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

//Ported from mamber, adjusted for public use,  credits to Matias

public class QuestHelper extends ResizableWnd {
    public boolean active = false;
    public QuestHelper.QuestList questList;

    public QuestHelper() {
        super(Coord.z, "Quest Helper");
        PButton btn = add(new PButton("Refresh"));
        questList = new QuestHelper.QuestList(UI.scale(270), 13, this);
        add(questList, UI.scale(0, btn.sz.y));
        makeHidable();
        pack();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == this.cbtn) {
            hide();
            disable();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public boolean type(char key, KeyEvent ev) {
        if (key == 27) {
            hide();
            disable();
            return true;
        } else {
            return super.type(key, ev);
        }
    }

    public void refresh() {
        if (active) {
            questList.temp.clear();
            questList.sb.val = questList.sb.min;
            questList.refresh = true;
        }
    }

    @Override
    public void close() {
        hide();
        disable();
    }

    public void addConds(List<QuestWnd.Quest.Condition> ncond, int id) {
        if (active) {
            boolean alltrue = true;

            for (int i = 0; i < ncond.size(); ++i) {
                QuestListItem qitem = new QuestListItem((ncond.get(i)).desc, (ncond.get(i)).done, id);
                if (alltrue && i == ncond.size() - 1) {
                    qitem = new QuestListItem("★ " + (ncond.get(i)).desc, 2, id);
                } else if ((ncond.get(i)).done == 1) {
                    qitem = new QuestListItem("✓ " + (ncond.get(i)).desc, (ncond.get(i)).done, id);
                } else {
                    alltrue = false;
                }

                boolean dontadd = false;
                Iterator var7 = questList.temp.iterator();

                while (var7.hasNext()) {
                    QuestListItem item = (QuestListItem) var7.next();
                    if (qitem.name.equals(item.name) && qitem.parentid == item.parentid) {
                        dontadd = true;
                        break;
                    }
                }

                if (!dontadd) {
                    questList.temp.add(qitem);
                }
            }

            questList.temp.sort(questList.comp);
            questList.quests = new ArrayList<>(questList.temp);
        }
    }

    private void disable() {
        this.active = false;
    }

    private static class QuestList extends Listbox<QuestListItem> {
        private static final Coord nameoff = UI.scale(0, 5);
        public List<QuestListItem> temp = Collections.synchronizedList(new ArrayList<>());
        public List<QuestListItem> quests = Collections.emptyList();
        public boolean refresh = true;
        private long lastUpdateTime = System.currentTimeMillis();
        private final Comparator<QuestListItem> comp = Comparator.comparing(a -> a.name);
        private QuestHelper questHelper;

        public QuestList(int w, int h, QuestHelper questHelper) {
            super(w, h, UI.scale(24));
            this.questHelper = questHelper;
        }

        @Override
        public void presize() {
            super.presize();
            super.resize(parent.xlate(parent.sz, false).sub(c).sub(UI.scale(5, 5)));
        }

        @Override
        public void tick(double dt) {
            if (ui.gui != null && ui.gui.menu != null) {
                if (questHelper.active) {
                    long timesincelastupdate = System.currentTimeMillis() - lastUpdateTime;
                    if (timesincelastupdate < 1000L) {
                        refresh = false;
                    }

                    if (ui != null && refresh) {
                        refresh = false;
                        lastUpdateTime = System.currentTimeMillis();
                        temp.clear();

                        try {
                            haven.chrwnd.QuestWnd qwnd = ui.gui.chrwdg.quest;
                            haven.chrwnd.SkillWnd swnd = ui.gui.chrwdg.skill;
                            if (qwnd != null && swnd != null)
                                qwnd.cqst.quests.forEach(quest -> {
                                    if (quest.id != ui.gui.chrwdg.skill.credos.pqid) {
                                        ui.gui.chrwdg.wdgmsg("qsel", quest.id);
                                    }
                                });
                            else
                                refresh = true;
                        } catch (NullPointerException var9) {
                            var9.printStackTrace();
                        } catch (Loading var10) {
                            refresh = true;
                            System.out.println("loading...");
                        }

                        selindex = -1;
                    }
                }
            }
        }

        @Override
        protected QuestListItem listitem(int idx) {
            return quests.get(idx);
        }

        @Override
        protected int listitems() {
            return quests.size();
        }

        @Override
        protected void drawbg(GOut g) {
            g.chcolor(0, 0, 0, 120);
            g.frect(Coord.z, this.sz);
            g.chcolor();
        }

        @Override
        protected void drawitem(GOut g, QuestListItem item, int idx) {
            try {
                if (item.status == 2) {
                    g.chcolor(new Color(0, 255, 0));
                } else if (item.status == 1) {
                    g.chcolor(new Color(0, 255, 255));
                } else {
                    g.chcolor(new Color(255, 255, 255));
                }

                g.text(item.name, nameoff);

                g.chcolor();
            } catch (Loading var5) {
            }
        }

        @Override
        public void change(QuestListItem item) {
            if (item != null) {
                super.change(item);
                ui.gui.chrwdg.wdgmsg("qsel", item.parentid);
            }
        }
    }

    private class PButton extends Button {
        public PButton(String title) {
            super(title);
        }

        @Override
        public void click() {
            QuestHelper.this.refresh();
        }
    }
}
