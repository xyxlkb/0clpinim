// util文件夹，用来放一些很基础的东西

package miniplc0java.util;


public class Pos {
    public Pos(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int row;
    public int col;

//    下一行的话会到下一行的开头 下一列的话其实就是这一行的下一个而已 2333 而且并没有什么超出行列范围的判断 在别处？
    public Pos nextCol() {
        return new Pos(row, col + 1);
    }

    public Pos nextRow() {
        return new Pos(row + 1, 0);
    }

    @Override
    public String toString() {
//        所以这么多append  其实就是把他们连接起来而已啊orz  怪我以前没见过
        return new StringBuilder().append("Pos(row: ").append(row).append(", col: ").append(col).append(")").toString();
    }

    public static void main(String[] args) {
        Pos position = new Pos(2, 3);
        System.out.println(position.nextCol().toString());
    }
}
