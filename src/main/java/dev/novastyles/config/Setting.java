package dev.novastyles.config;

public abstract class Setting {
    public final String id;
    public final String label;

    protected Setting(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public abstract String serialize();
    public abstract void deserialize(String raw);
    public abstract void reset();

    public static final class Bool extends Setting {
        public final boolean def;
        public boolean value;

        public Bool(String id, String label, boolean def) {
            super(id, label);
            this.def = def;
            this.value = def;
        }

        @Override public String serialize() { return Boolean.toString(value); }
        @Override public void deserialize(String raw) { value = Boolean.parseBoolean(raw); }
        @Override public void reset() { value = def; }
    }

    public static final class Mode extends Setting {
        public final String[] options;
        public final int def;
        public int index;

        public Mode(String id, String label, int def, String... options) {
            super(id, label);
            this.options = options;
            this.def = def;
            this.index = def;
        }

        public String current() { return options[index]; }

        public void cycle(int dir) { index = Math.floorMod(index + dir, options.length); }

        @Override public String serialize() { return options[index]; }

        @Override
        public void deserialize(String raw) {
            for (int i = 0; i < options.length; i++) {
                if (options[i].equalsIgnoreCase(raw)) { index = i; return; }
            }
        }

        @Override public void reset() { index = def; }
    }

    public static final class Num extends Setting {
        public final float min, max, def, step;
        public float value;

        public Num(String id, String label, float min, float max, float def, float step) {
            super(id, label);
            this.min = min;
            this.max = max;
            this.def = def;
            this.step = step;
            this.value = def;
        }

        public void setClamped(float v) {
            v = Math.round(v / step) * step;
            value = Math.max(min, Math.min(max, v));
        }

        @Override public String serialize() { return Float.toString(value); }

        @Override
        public void deserialize(String raw) {
            try { setClamped(Float.parseFloat(raw)); } catch (NumberFormatException ignored) { }
        }

        @Override public void reset() { value = def; }
    }
}
