public class ClassWithWarnings {
    private double x = 1.0;
    private double y = 2.0;
    public double slope = x / y; // will be skipped from validation due to forward reference

    public int[] array() {
        int[] a = new int[]
                {
                        1,
                        2,
                        3,
                };
        return a;
    }
}