package week05;


public class ShipCalculator {


    // fee(int km) → 回傳 km * 50
    public int fee(int km) {
        return km * 50;
    }

    //
    // fee(double km) → 回傳 km * 50.0
    public double fee(double km) {
        return km * 50.0;
    }

  
    // fee(int km, double weight) → 回傳 km * 50 + weight * 10
    public double fee(int km, double weight) {
        return km * 50 + weight * 10;
    }

  
    // travelTime(int km) → 回傳 km / 30.0（時速 30 公里）
    // travelTime(int km, int speed) → 回傳 km / (double)speed
    public double travelTime(int km) {
        return km / 30.0;
    }

    public double travelTime(int km, int speed) {
        return km / (double) speed;
    }

    public static void main(String[] args) {
        ShipCalculator calc = new ShipCalculator();

        System.out.println("=== 船舶運費計算器 ===");
        System.out.println("100 公里運費：" + calc.fee(100));
        System.out.println("100.5 公里運費：" + calc.fee(100.5));
        System.out.println("100 公里 + 500kg 運費：" + calc.fee(100, 500.0));
        System.out.println("100 公里航行時間：" +
            String.format("%.1f", calc.travelTime(100)) + " 小時");
        System.out.println("100 公里（時速 20）：" +
            String.format("%.1f", calc.travelTime(100, 20)) + " 小時");
    }
}
