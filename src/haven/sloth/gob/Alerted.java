package haven.sloth.gob;

import com.google.common.flogger.FluentLogger;
import haven.Audio;
import haven.Config;
import haven.Gob;
import haven.Resource;
import haven.UI;
import haven.purus.pbot.PBotDiscord;
import haven.sloth.io.Storage;
import haven.sloth.util.ObservableCollection;
import haven.sloth.util.ObservableListener;
import modification.configuration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//TODO: Idealy all the sounds we allow should be stored locally and separate from jorb's names to avoid issues in the future
public class Alerted {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    public static final Map<String, Boolean> customsort = Collections.synchronizedMap(new HashMap<>());
    public static final ObservableCollection<ConnectSound> connectList = new ObservableCollection<>(new HashSet<>());

    //    private static final ObservableMap<String, String> soundmap = new ObservableMap<>(new TreeMap<>());
//    private static final ObservableMap<String, String> sfxmap = new ObservableMap<>(new TreeMap<>());
//    public static ObservableMap<String, Double> volmap = new ObservableMap<>(new TreeMap<>());
    public static final List<String> custom = Collections.synchronizedList(new ArrayList<>());
    public static final List<String> sounds = Collections.synchronizedList(new ArrayList<>());
    public static List<String> localsounds = Collections.synchronizedList(new ArrayList<>());
    private static Set<Long> sgobs = new HashSet<Long>();
    private static Map<Long, Long> alertedmap = new HashMap<>(); //map for the purposes of tracking gobs/alerts to ensure alerts only first with a minimum of a 1 second delay

    public static void init(final Storage internal) {
        Storage.dynamic.ensure((sql) -> {
            try (final Statement stmt = sql.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS gob_sound ( name TEXT PRIMARY KEY, sfx TEXT, volume DOUBLE )");
            }
        });
        Storage.dynamic.ensure((sql) -> {
            try (final Statement stmt = sql.createStatement()) {
                try (final ResultSet res = stmt.executeQuery("SELECT name, sfx, volume FROM gob_sound")) {
                    while (res.next()) {
                        connectList.add(new ConnectSound(res.getString(1), res.getString(2), res.getDouble(3), false));
//                        sfxmap.put(res.getString(1), res.getString(2));
//                        volmap.put(res.getString(1), res.getDouble(3));
                    }
                }
            }
        });
        Storage.dynamic.ensure((sql) -> {
            try (final Statement stmt = sql.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS gob_localsound ( name TEXT PRIMARY KEY, sound TEXT, volume DOUBLE )");
            }
        });
        Storage.dynamic.ensure((sql) -> {
            try (final Statement stmt = sql.createStatement()) {
                try (final ResultSet res = stmt.executeQuery("SELECT name, sound, volume FROM gob_localsound")) {
                    while (res.next()) {
                        connectList.add(new ConnectSound(res.getString(1), res.getString(2), res.getDouble(3), true));
//                        soundmap.put(res.getString(1), res.getString(2));
//                        volmap.put(res.getString(1), res.getDouble(3));
                    }
                }
            }
        });
        localsounds = configuration.findFiles(configuration.soundPath, Arrays.asList(".wav"));
        internal.ensure((sql) -> {
            try (final Statement stmt = sql.createStatement()) {
                try (final ResultSet res = stmt.executeQuery("SELECT name, volume FROM alerts WHERE type_id = (SELECT type_id FROM type WHERE name_key = 'SOUND')")) {
                    while (res.next()) {
                        sounds.add(res.getString(1));
//                        if (!volmap.containsKey(res.getString(1)))
//                            volmap.put(res.getString(1), res.getDouble(2));
                    }
                }
            }
            sounds.sort(String::compareTo);
        });
        for (final String sound : sounds) {
            customsort.put(sound, false);
            custom.add(sound);
        }
        for (String sound : localsounds) {
            customsort.put(sound, true);
            custom.add(sound);
        }
        custom.sort(String::compareTo);

        for (final String sound : sounds) {
            try {
                Resource.local().load(sound);
            } catch (Throwable e) {
                //Ignore it
                logger.atSevere().withCause(e).log("Failed to load %s", sound);
            }
        }
    }


    public static synchronized void remove(final String name) {
        if (containsObj(name)) {
            if (isLocal(name))
                Storage.dynamic.write(sql -> {
                    final PreparedStatement stmt = Storage.dynamic.prepare("DELETE FROM gob_localsound WHERE name = ?");
                    stmt.setString(1, name);
                    stmt.executeUpdate();
                });
            else
                Storage.dynamic.write(sql -> {
                    final PreparedStatement stmt = Storage.dynamic.prepare("DELETE FROM gob_sound WHERE name = ?");
                    stmt.setString(1, name);
                    stmt.executeUpdate();
                });
            removeConnect(name);
        }
    }

    public static synchronized void add(final String name, final String sound, final Double volume) {
        if (!(containsObj(name) && getSound(name).equals(sound) && getVolume(name).equals(volume))) {
            //Only update if we have to.
            connectList.add(new ConnectSound(name, sound, volume));
        } else {
            if (isLocal(name) != customsort.get(sound)) remove(name);
            getConnectSound(name).edit(name, sound, volume);
        }
//                soundmap.put(name, sound);
//                volmap.put(name, volume);
        if (isLocal(name))
            Storage.dynamic.write(sql -> {
                final PreparedStatement stmt = Storage.dynamic.prepare("INSERT OR REPLACE INTO gob_localsound VALUES (?, ?, ?)");
                stmt.setString(1, name);
                stmt.setString(2, sound);
                stmt.setDouble(3, volume);
                stmt.executeUpdate();
            });
        else
            Storage.dynamic.write(sql -> {
                final PreparedStatement stmt = Storage.dynamic.prepare("INSERT OR REPLACE INTO gob_sound VALUES (?, ?, ?)");
                stmt.setString(1, name);
                stmt.setString(2, sound);
                stmt.setDouble(3, volume);
                stmt.executeUpdate();
            });
    }

    public synchronized static void listen(final ObservableListener<ConnectSound> listener) {
        connectList.addListener(listener);
//        soundmap.addListener(listener);
//        sfxmap.addListener(listener);
    }

    public synchronized static void unlisten(final ObservableListener<ConnectSound> listener) {
        connectList.removeListener(listener);
//        soundmap.removeListener(listener);
//        sfxmap.removeListener(listener);
    }

    public static void checkAlert(final String name, final Gob g) {
        try {
            if (containsObj(name)) {
                if (!name.equals("gfx/borka/body") && !g.isDead() && !sgobs.contains(g.id)) {
                    if (!alertedmap.containsKey(g.id) || (System.currentTimeMillis() - alertedmap.get(g.id) > 5000)) {
                        try {
                            if (isLocal(name))
                                Audio.play(getSound(name), getVolume(name));
                            else
                                Audio.play(Resource.local().load(getSound(name)), getVolume(name));
                            if (Config.discordalarmalert) {
                                if (g.glob.ui != null) {
                                    UI ui = g.glob.ui.get();
                                    if (!configuration.endpoint.isEmpty() && ui != null && ui.sess != null && ui.sess.alive() && ui.sess.username != null && ui.gui != null) {
                                        String s = g.name().substring(g.name().lastIndexOf("/") + 1);
                                        if (!ui.gui.chrid.isEmpty()) {
                                            String username = ui.sess.username + "/" + ui.gui.chrid;
                                            if (Config.discorduser) {
                                                PBotDiscord.mapAlert(username, Config.discordalertstring, s);
                                            } else if (Config.discordrole) {
                                                PBotDiscord.mapAlertRole(username, Config.discordalertstring, s);
                                            } else {
                                                PBotDiscord.mapAlertEveryone(username, s);
                                            }
                                        }
                                    }
                                }
                            }
                            if (Config.alarmonce) {
                                sgobs.add(g.id);
                            }
                            alertedmap.put(g.id, System.currentTimeMillis());
                        } catch (Throwable e) {
                            e.printStackTrace();
                            System.out.println("[Alerted checkAlert] Sound: " + getConnectSound(name).toString() + " Gob: " + g.getres());
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }//crashing during an alarm would be bad
    }


    public static boolean shouldAlert(final String name) {
        return (containsObj(name));
    }

    public static class ConnectSound {
        public String objName;
        public String soundName;
        public Double volume;
        public boolean local;

        public ConnectSound(String objName, String soundName, double volume) {
            this(objName, soundName, volume, customsort.get(soundName));
        }

        public ConnectSound(String objName, String soundName, double volume, boolean local) {
            this.objName = objName;
            this.soundName = soundName;
            this.volume = volume;
            this.local = local;
        }

        public void edit(String objName, String soundName, double volume) {
            this.objName = objName;
            this.soundName = soundName;
            this.volume = volume;
            this.local = customsort.get(soundName);
        }

        public String toString() {
            return "[ObjName: " + objName + "] [SoundName: " + soundName + "] [Volume: " + volume + "] [IsLocal: " + local + "]";
        }
    }

    public static boolean containsObj(String name) {
        for (ConnectSound sound : connectList)
            if (name.equals(sound.objName))
                return true;
        return false;
    }

    public static void removeConnect(String name) {
        for (ConnectSound sound : connectList)
            if (name.equals(sound.objName)) {
                connectList.remove(sound);
                break;
            }
    }

    public static boolean isLocal(String name) {
        for (ConnectSound sound : connectList)
            if (name.equals(sound.objName))
                return sound.local;
        return false;
    }

    public static ConnectSound getConnectSound(String name) {
        for (ConnectSound sound : connectList)
            if (name.equals(sound.objName))
                return sound;
        return null;
    }

    public static String getSound(String name) {
        for (ConnectSound sound : connectList)
            if (name.equals(sound.objName))
                return sound.soundName;
        return null;
    }

    public static Double getVolume(String name) {
        for (ConnectSound sound : connectList)
            if (name.equals(sound.objName))
                return sound.volume;
        return .8;
    }
}
