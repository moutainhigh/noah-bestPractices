package org.geekbang.time.commonmistakes.java8;

import org.geekbang.time.commonmistakes.io.filestreamoperationneedclose.NoahResource;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CoolStreamTest {

    private Map<Long, Product> cache = new ConcurrentHashMap<>();

    /**
     * 业务功能：普通实现
     * <p>
     * 把整数列表转换为 Point2D 列表;
     * 遍历 Point2D 列表过滤出 Y 轴 >1 的对象;
     * 计算 Point2D 点到原点的距离;
     * 累加所有计算出的距离，并计算距离的平均值。
     *
     * @param ints
     * @return
     */
    private static double calc(List<Integer> ints) {
        //临时中间集合
        List<Point2D> point2DList = new ArrayList<>();
        for (Integer i : ints) {
            point2DList.add(new Point2D.Double((double) i % 3, (double) i / 3));
        }
        //临时变量,纯粹是为了获得最后结果需要的中间变量
        double total = 0;
        int count = 0;

        for (Point2D point2D : point2DList) {
            //过滤
            if (point2D.getY() > 1) {
                //算距离
                double distance = point2D.distance(0, 0);
                total += distance;
                count++;
            }
        }
        return count > 0 ? total / count : 0;
    }

    /**
     * 使用Java8 stream遍历集合
     * <p>
     * map 方法传入的是一个 Function，可以实现对象转换;
     * filter 方法传入一个 Predicate，实现对象的布尔判断，只保留返回 true 的数据;
     * mapToDouble 用于把对象转换为 double;
     * 通过 average 方法返回一个 OptionalDouble，代表可能包含值也可能不包含值的可空 double。
     */
    @Test
    public void stream() {
        List<Integer> ints = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        double average = calc(ints);
        double streamResult = ints.stream()
                .map(i -> new Point2D.Double((double) i % 3, (double) i / 3))
                .filter(point -> point.getY() > 1)
                .mapToDouble(point -> point.distance(0, 0))
                .average()
                .orElse(0);
        //如何用一行代码来实现,比较一下可读性
        assertThat(average, is(streamResult));
    }


    /**
     * java8使用函数式，增强方法
     * <p>
     * 一条语句实现cache的常用模式
     */
    @Test
    public void coolCache() {
        System.out.println(getProductAndCacheCool(1L));
        getProductAndCacheCool(100L);

        System.out.println("第二次获取id=1，结果=" + getProductAndCacheCool(1L));

        System.out.println(cache);
        assertThat(cache.size(), is(1));
        assertTrue(cache.containsKey(1L));
    }

    @Test
    public void notcoolCache() {
        getProductAndCache(1L);
        getProductAndCache(100L);

        System.out.println(cache);
        assertThat(cache.size(), is(1));
        assertTrue(cache.containsKey(1L));
    }

    private Product getProductAndCacheCool(Long id) {

        //todo:实现value=null，也保存进去，缓存穿透，要看Map的具体实现，当前是ConrecntHashMap，看下HashMap

        return cache.computeIfAbsent(id, i -> //当Key不存在的时候提供一个Function来代表根据Key获取Value的过程
                Product.getData().stream()
                        .filter(p -> p.getId().equals(i)) //过滤
                        .findFirst() //找第一个，得到Optional<Product>
                        .orElse(null)); //如果找不Product到则使用null
    }

    private Product getProductAndCache(Long id) {
        Product product = null;
        if (cache.containsKey(id)) {
            product = cache.get(id);
        } else {
            for (Product p : Product.getData()) {
                if (p.getId().equals(id)) {
                    product = p;
                    break;
                }
            }
            if (product != null)
                cache.put(id, product);
        }
        return product;
    }

    @Test
    public void filesExample() throws IOException {
        //无限深度，递归遍历文件夹
        try (Stream<Path> pathStream = Files.walk(Paths.get("."))) {
            pathStream.filter(Files::isRegularFile) //只查普通文件
                    .filter(FileSystems.getDefault().getPathMatcher("glob:**/*.java")::matches) //搜索java源码文件
                    .flatMap(ThrowingFunction.unchecked(path ->
                            Files.readAllLines(path).stream() //读取文件内容，转换为Stream<List>
                                    .filter(line -> Pattern.compile("public class").matcher(line).find()) //使用正则过滤带有public class的行
                                    .map(line -> path.getFileName() + " >> " + line))) //把这行文件内容转换为文件名+行
                    .forEach(System.out::println); //打印所有的行
        }
    }

    @Test
    public void noahTest() {
        try (NoahResource c = new NoahResource()) {
            throw new RuntimeException("发生了xxx具体的错误");
        }
    }

    /**
     * 斐波那契数列，java8实现
     * todo：原理
     */
    @Test
    public void fibonacci() {
        Stream.iterate(new BigInteger[]{BigInteger.ONE, BigInteger.ONE},
                p -> new BigInteger[]{p[1], p[0].add(p[1])})
                .limit(100)
                .forEach(p -> System.out.println(p[0]));
    }

    /**
     * 函数式接口：优雅处理受检异常
     *
     * @param <T>
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R, E extends Throwable> {
        static <T, R, E extends Throwable> Function<T, R> unchecked(ThrowingFunction<T, R, E> f) {
            return t -> {
                try {
                    return f.apply(t);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
        }

        R apply(T t) throws E;
    }
}
