package week05;

// TODO 2: Ship 類別
// - 屬性：name
// - 建構子接收 name
// - final 方法 type()：回傳「船舶」
// - 一般方法 sail()：回傳 name + " 正在航行"
class Ship {
    protected String name;

    public Ship(String name) {
        this.name = name;
    }

    public final String type() {
        return "船舶";
    }

    public String sail() {
        return name + " 正在航行";
    }
}

// TODO 3: FishingBoat 繼承 Ship
// - 建構子用 super(name)
// - 覆寫 sail()：回傳 name + " 正在拖網捕魚"
// - 嘗試覆寫 type()，觀察編譯錯誤，然後註解掉
class FishingBoat extends Ship {
    public FishingBoat(String name) {
        super(name);
    }

    @Override
    public String sail() {
        return name + " 正在拖網捕魚";
    }

    // @Override
    // public String type() { // 編譯錯誤：Cannot override the final method from Ship
    //     return "漁船";
    // }
}

public class FinalDemo {
    // TODO 1: 宣告 final 變數
    static final int MAX_DEPTH = 11034;  // 馬里亞納海溝最深處

    public static void main(String[] args) {
        System.out.println("馬里亞納海溝最深：" + MAX_DEPTH + " 公尺");

        // MAX_DEPTH = 12000;  // 取消註解會發生編譯錯誤：The final field FinalDemo.MAX_DEPTH cannot be assigned

        Ship s = new Ship("遠洋號");
        FishingBoat f = new FishingBoat("海豐號");

        System.out.println(s.type() + "：" + s.sail());
        System.out.println(f.type() + "：" + f.sail());

        // 多型
        Ship s2 = new FishingBoat("福星號");
        System.out.println(s2.type() + "：" + s2.sail());
    }
}
