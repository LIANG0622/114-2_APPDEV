package week05;

// 1. 父類別 Creature
class Creature {
    private String name;
    private String habitat;

    public Creature(String name, String habitat) {
        this.name = name;
        this.habitat = habitat;
    }

    public String move() {
        return getName() + " 一般移動";
    }

    public String eat() {
        return getName() + " 一般覓食";
    }

    public String describe() {
        return getName() + "（" + getHabitat() + "）";
    }

    // final 方法：子類別無法覆寫
    public final String kingdom() {
        return "動物界";
    }

    // 3. 方法多載 feed()
    public String feed() {
        return getName() + " 正在覓食";
    }

    public String feed(String food) {
        return getName() + " 正在吃 " + food;
    }

    public String feed(String food, int amount) {
        return getName() + " 吃了 " + amount + " 份 " + food;
    }

    // 為了更好的封裝，將屬性設為 private，並提供 getter
    public String getName() {
        return name;
    }

    public String getHabitat() {
        return habitat;
    }
}

// 2. 子類別 1：Shark
class Shark extends Creature {
    public Shark(String name, String habitat) {
        super(name, habitat);
    }

    @Override
    public String move() { return getName() + " 高速衝刺獵食"; }

    @Override
    public String eat() { return getName() + " 撕咬獵物"; }
}

// 2. 子類別 2：Turtle
class Turtle extends Creature {
    public Turtle(String name, String habitat) {
        super(name, habitat);
    }

    @Override
    public String move() { return getName() + " 緩慢划動四肢"; }

    @Override
    public String eat() { return getName() + " 啃食海草"; }
}

// 2. 子類別 3：Dolphin
class Dolphin extends Creature {
    public Dolphin(String name, String habitat) {
        super(name, habitat);
    }

    @Override
    public String move() { return getName() + " 躍出水面再潛入"; }

    @Override
    public String eat() { return getName() + " 合作圍捕魚群"; }
}

// 2. 子類別 4：Octopus
class Octopus extends Creature {
    public Octopus(String name, String habitat) {
        super(name, habitat);
    }

    @Override
    public String move() { return getName() + " 噴射水流推進"; }

    @Override
    public String eat() { return getName() + " 用觸手捕捉獵物"; }
}

public class MarineEcosystem {
    public static void main(String[] args) {
        // 4. final 變數使用
        final int OCEAN_DEPTH = 11034;
        System.out.println("海洋最深處：" + OCEAN_DEPTH + " 公尺\n");

        // 5. main 中展示多型
        Creature[] ecosystem = {
            new Shark("大白鯊", "深海"),
            new Turtle("綠蠵龜", "珊瑚礁"),
            new Dolphin("瓶鼻海豚", "近海"),
            new Octopus("藍環章魚", "深海")
        };

        for (Creature c : ecosystem) {
            System.out.println(c.describe());
            System.out.println("  分類：" + c.kingdom());
            System.out.println("  移動：" + c.move());
            System.out.println("  覓食：" + c.eat());
            System.out.println("  餵食：" + c.feed());
            System.out.println("  餵食：" + c.feed("小魚"));
            System.out.println("  餵食：" + c.feed("小魚", 3));
            System.out.println();
        }
    }
}