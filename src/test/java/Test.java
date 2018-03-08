import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by tianqi on 17/9/7.
 */
public class Test {
    public static void main(String[] args) {
        Formula a=new For();
        a.calculate(1);
        System.out.println(a.sqrt(8));

        List<String> names = Arrays.asList("peter", "anna", "mike", "xenia");
        Collections.sort(names,(String c,String b) -> {
            return b.compareTo(c);
        });

        for(String name:names){

            System.out.println(name);
        }
    }
    interface Formula {
        double calculate(int a);
        default double sqrt(int a) {//这个方法可以在实现类重写，或者直接使用
            return Math.sqrt(a);
        }
    }
    static class For implements Formula{
        @Override
        public double calculate(int a) {
            return 0;
        }

        @Override
        public double sqrt(int a) {
            return 0;
        }
    }
}
