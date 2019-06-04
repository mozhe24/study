<!-- GFM-TOC -->

* [一、概览](#一概览)
* [二、磁盘操作](#二磁盘操作)
* [三、字节操作](#三字节操作)
    * [实现文件复制](#实现文件复制)
    * [装饰者模式](#装饰者模式)
* [四、字符操作](#四字符操作)
    * [编码与解码](#编码与解码)
    * [String 的编码方式](#string-的编码方式)
    * [Reader 与 Writer](#reader-与-writer)
    * [实现逐行输出文本文件的内容](#实现逐行输出文本文件的内容)
* [五、对象操作](#五对象操作)
    * [序列化](#序列化)
    * [Serializable](#serializable)
    * [transient](#transient)
* [六、网络操作](#六网络操作)
    * [InetAddress](#inetaddress)
    * [URL](#url)
    * [Sockets](#sockets)
    * [Datagram](#datagram)
* [七、NIO](#七nio)
    * [流与块](#流与块)
    * [通道与缓冲区](#通道与缓冲区)
    * [缓冲区状态变量](#缓冲区状态变量)
    * [文件 NIO 实例](#文件-nio-实例)
    * [选择器](#选择器)
    * [套接字 NIO 实例](#套接字-nio-实例)
    * [内存映射文件](#内存映射文件)
    * [对比](#对比)
* [八、AIO](#八AIO)
    * [io.netty.buffer和java.nio.ByteBuffer的区别](#io.netty.buffer和java.nio.ByteBuffer的区别)
    * [Netty的零拷贝(zero copy)](#Netty的零拷贝(zero copy))

<!-- GFM-TOC -->


# 一、概览

`Java` 的` I/O `大概可以分成以下几类：

- 磁盘操作：`File`
- 字节操作：`InputStream `和` OutputStream`
- 字符操作：`Reader `和` Writer`
- 对象操作：`Serializable`
- 网络操作：`Socket`
- 新的输入/输出：`NIO`



相关`IO`区别：

* `BIO （Blocking I/O）`：同步阻塞I/O模式，数据的读取写入必须阻塞在一个线程内等待其完成。这里使用那个经典的烧开水例子，这里假设一个烧开水的场景，有一排水壶在烧开水，`BIO`的工作模式就是， 叫一个线程停留在一个水壶那，直到这个水壶烧开，才去处理下一个水壶。但是实际上线程在等待水壶烧开的时间段什么都没有做。

* `NIO （New I/O）`：同时支持阻塞与非阻塞模式，但这里我们以其同步非阻塞I/O模式来说明，那么什么叫做同步非阻塞？如果还拿烧开水来说，`NIO`的做法是叫一个线程不断的轮询每个水壶的状态，看看是否有水壶的状态发生了改变，从而进行下一步的操作。

* `AIO （ Asynchronous I/O）`：异步非阻塞I/O模型。异步非阻塞与同步非阻塞的区别在哪里？异步非阻塞无需一个线程去轮询所有IO操作的状态改变，在相应的状态改变后，系统会通知对应的线程来处理。对应到烧开水中就是，为每个水壶上面装了一个开关，水烧开之后，水壶会自动通知我水烧开了。

# 二、磁盘操作

`File` 类可以用于表示文件和目录的信息，但是它不表示文件的内容。

递归地列出一个目录下所有文件：

```java
public static void listAllFiles(File dir) {
    if (dir == null || !dir.exists()) {
        return;
    }
    if (dir.isFile()) {
        System.out.println(dir.getName());
        return;
    }
    for (File file : dir.listFiles()) {
        listAllFiles(file);
    }
}
```

从` Java7 `开始，可以使用 `Paths` 和 `Files `代替` File`。

# 三、字节操作

## 实现文件复制

```java
public static void copyFile(String src, String dist) throws IOException {
    FileInputStream in = new FileInputStream(src);
    FileOutputStream out = new FileOutputStream(dist);
    byte[] buffer = new byte[20 * 1024];
    int cnt;
    // read() 最多读取 buffer.length 个字节
    // 返回的是实际读取的个数
    // 返回 -1 的时候表示读到 eof，即文件尾
    while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
        out.write(buffer, 0, cnt);
    }
    in.close();
    out.close();
}
```

## 装饰者模式

`Java I/O` 使用了装饰者模式来实现。以` InputStream `为例，

- `InputStream `是抽象组件；
- `FileInputStream` 是` InputStream `的子类，属于具体组件，提供了字节流的输入操作；
- `FilterInputStream `属于抽象装饰者，装饰者用于装饰组件，为组件提供额外的功能。例如 `BufferedInputStream` 为 `FileInputStream` 提供缓存的功能。

![1](./assert/1.png)

实例化一个具有缓存功能的字节流对象时，只需要在 `FileInputStream `对象上再套一层 `BufferedInputStream `对象即可。

```java
FileInputStream fileInputStream = new FileInputStream(filePath);
BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
```

`DataInputStream `装饰者提供了对更多数据类型进行输入的操作，比如 `int、double `等基本类型。

# 四、字符操作

## 编码与解码

编码就是把字符转换为字节，而解码是把字节重新组合成字符。

如果编码和解码过程使用不同的编码方式那么就出现了乱码。

- `GBK` 编码中，中文字符占 2 个字节，英文字符占 1 个字节；
- `UTF-8` 编码中，中文字符占 3 个字节，英文字符占 1 个字节；
- `UTF-16be `编码中，中文字符和英文字符都占 2 个字节。

`UTF-16be `中的` be `指的是` Big Endian`，也就是大端。相应地也有` UTF-16le`，`le `指的是 `Little Endian`，也就是小端。

Java 的内存编码使用双字节编码 UTF-16be，这不是指 Java 只支持这一种编码方式，而是说 char 这种类型使用 UTF-16be 进行编码。char 类型占 16 位，也就是两个字节，Java 使用这种双字节编码是为了让一个中文或者一个英文都能使用一个 char 来存储。

## String 的编码方式

String 可以看成一个字符序列，可以指定一个编码方式将它编码为字节序列，也可以指定一个编码方式将一个字节序列解码为 String。

```java
String str1 = "中文";
byte[] bytes = str1.getBytes("UTF-8");
String str2 = new String(bytes, "UTF-8");
System.out.println(str2);
```

在调用无参数 getBytes() 方法时，默认的编码方式不是 UTF-16be。双字节编码的好处是可以使用一个 char 存储中文和英文，而将 String 转为 bytes[] 字节数组就不再需要这个好处，因此也就不再需要双字节编码。getBytes() 的默认编码方式与平台有关，一般为 UTF-8。

```java
byte[] bytes = str1.getBytes();
```

## Reader 与 Writer

不管是磁盘还是网络传输，最小的存储单元都是字节，而不是字符。但是在程序中操作的通常是字符形式的数据，因此需要提供对字符进行操作的方法。

- InputStreamReader 实现从字节流解码成字符流；
- OutputStreamWriter 实现字符流编码成为字节流。

## 实现逐行输出文本文件的内容

```java
public static void readFileContent(String filePath) throws IOException {
    FileReader fileReader = new FileReader(filePath);
    BufferedReader bufferedReader = new BufferedReader(fileReader);
    String line;
    while ((line = bufferedReader.readLine()) != null) {
        System.out.println(line);
    }
    // 装饰者模式使得 BufferedReader 组合了一个 Reader 对象
    // 在调用 BufferedReader 的 close() 方法时会去调用 Reader 的 close() 方法
    // 因此只要一个 close() 调用即可
    bufferedReader.close();
}
```

# 五、对象操作

## 序列化

序列化就是将一个对象转换成字节序列，方便存储和传输。

- 序列化：ObjectOutputStream.writeObject()
- 反序列化：ObjectInputStream.readObject()

不会对静态变量进行序列化，因为序列化只是保存对象的状态，静态变量属于类的状态。

## Serializable

序列化的类需要实现 Serializable 接口，它只是一个标准，没有任何方法需要实现，但是如果不去实现它的话而进行序列化，会抛出异常。

```java
public static void main(String[] args) throws IOException, ClassNotFoundException {

    A a1 = new A(123, "abc");
    String objectFile = "file/a1";

    ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(objectFile));
    objectOutputStream.writeObject(a1);
    objectOutputStream.close();

    ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(objectFile));
    A a2 = (A) objectInputStream.readObject();
    objectInputStream.close();
    System.out.println(a2);
}

private static class A implements Serializable {

    private int x;
    private String y;

    A(int x, String y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "x = " + x + "  " + "y = " + y;
    }
}
```

## transient

transient 关键字可以使一些属性不会被序列化。

ArrayList 中存储数据的数组 elementData 是用 transient 修饰的，因为这个数组是动态扩展的，并不是所有的空间都被使用，因此就不需要所有的内容都被序列化。通过重写序列化和反序列化方法，使得可以只序列化数组中有内容的那部分数据。

```java
private transient Object[] elementData;
```

# 六、网络操作

Java 中的网络支持：

- InetAddress：用于表示网络上的硬件资源，即 IP 地址；
- URL：统一资源定位符；
- Sockets：使用 TCP 协议实现网络通信；
- Datagram：使用 UDP 协议实现网络通信。

## InetAddress

没有公有的构造函数，只能通过静态方法来创建实例。

```java
InetAddress.getByName(String host);
InetAddress.getByAddress(byte[] address);
```

## URL

可以直接从 URL 中读取字节流数据。

```java
public static void main(String[] args) throws IOException {
    URL url = new URL("http://www.baidu.com");
    /* 字节流 */
    InputStream is = url.openStream();
    /* 字符流 */
    InputStreamReader isr = new InputStreamReader(is, "utf-8");
    /* 提供缓存功能 */
    BufferedReader br = new BufferedReader(isr);
    String line;
    while ((line = br.readLine()) != null) {
        System.out.println(line);
    }
    br.close();
}
```

## Sockets

- ServerSocket：服务器端类
- Socket：客户端类
- 服务器和客户端通过 InputStream 和 OutputStream 进行输入输出。

![2](./assert/2.jpg)

## Datagram

- DatagramSocket：通信类
- DatagramPacket：数据包类

# 七、NIO

新的输入/输出 (NIO) 库是在 JDK 1.4 中引入的，弥补了原来的 I/O 的不足，提供了高速的、面向块的 I/O。

## 流与块

I/O 与 NIO 最重要的区别是数据打包和传输的方式，I/O 以流的方式处理数据，而 NIO 以块的方式处理数据。

面向流的 I/O 一次处理一个字节数据：一个输入流产生一个字节数据，一个输出流消费一个字节数据。为流式数据创建过滤器非常容易，链接几个过滤器，以便每个过滤器只负责复杂处理机制的一部分。不利的一面是，面向流的 I/O 通常相当慢。

面向块的 I/O 一次处理一个数据块，按块处理数据比按流处理数据要快得多。但是面向块的 I/O 缺少一些面向流的 I/O 所具有的优雅性和简单性。

I/O 包和 NIO 已经很好地集成了，java.nio.\* 已经以 NIO 为基础重新实现了，所以现在它可以利用 NIO 的一些特性。例如，java.nio.\* 包中的一些类包含以块的形式读写数据的方法，这使得即使在面向流的系统中，处理速度也会更快。

## 通道与缓冲区

### 1. 通道

* 用于源节点与目标节点的连接，在Java NIO中，负责缓冲区中数据的传输。其本身不存储任何数据，需要配合缓冲区进行传输

* 通道的主要实现类
    java.nio.channels.Channel接口：
    FileChannel（用于本地）
    SocketChannel（用于网络TCP）
    ServerSocketChannel（用于网络TCP）
    DatagramChannel（用于网络UDP）

* 获取通道
    1、java针对支持通道的类提供了getChannel方法
    本地IO：
    FileInputStream/FileOutputStream
    RandomAccessFile
    网络IO：
    Socket
    ServerSocket
    DatagramSocket
    2、在JDK1.7中的NIO2针对各个通道提供了静态方法open()方法
    3、在JDK1.7中的NIO2的Files工具类的newByteChannel()方法

* 通道之间的数据传输
    transferFrom()、transferTo()

    ```java
    /**
     * 使用channel完成一个文件的复制，这是一种最简单的复制方式
     */
    public void test1() {
    	FileChannel inChannel = null;
    	FileChannel outChannel = null;
    	try {
    		inChannel = new FileInputStream("D:\\1.txt").getChannel();
    		outChannel = new FileOutputStream("D:\\2.txt").getChannel();
    		ByteBuffer buffer = ByteBuffer.allocate(1024);
    		while (inChannel.read(buffer) != -1) {
    			buffer.flip();
    			outChannel.write(buffer);
    			buffer.clear();
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	} finally {
    		try {
    			if (inChannel != null) {
    				inChannel.close();
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		try {
    			if (outChannel != null) {
    				outChannel.close();
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    }
    
    /**
     * 使用内存映射进行文件复制
     */
    public void test2() throws IOException {
    	Instant start = Instant.now();
    	//后一个参数规定文件的读写权限
    	FileChannel inChannel = FileChannel.open(Paths.get("D:\\1.txt"), StandardOpenOption.READ);
    	//CREATE_NEW表示一定要新创建，而CREATE表示没有才创建
    	FileChannel outChannel = FileChannel.open(Paths.get("D:\\2.txt"), StandardOpenOption.WRITE,
    			StandardOpenOption.READ, StandardOpenOption.CREATE);
    
    	//这是一个内存映射，和ByteBuffer.allocateDirect(1024)是一样的道理
    	//内存映射只有ByteBuffer支持
    	MappedByteBuffer inMappedByteBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
    	//因为这里是读写模式，所以上面的outChannel也必须要有读模式
    	MappedByteBuffer outMappedByteBuffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0, inChannel.size());
    
    	byte[] bytes = new byte[inMappedByteBuffer.limit()];
    	inMappedByteBuffer.get(bytes);
    	outMappedByteBuffer.put(bytes);
    
    	inChannel.close();
    	outChannel.close();
    	Instant end = Instant.now();
    	System.out.println("耗时： " + Duration.between(start, end));
    }
    
    /**
     * 通道之间直接传输
     */
    public void test3() throws IOException {
    	FileChannel inChannel = FileChannel.open(Paths.get("D:\\1.txt"), StandardOpenOption.READ);
    	FileChannel outChannel = FileChannel.open(Paths.get("D:\\2.txt"), StandardOpenOption.WRITE,
    			StandardOpenOption.READ, StandardOpenOption.CREATE);
    
    	//	inChannel.transferTo(0, inChannel.size(), outChannel);
    	//和上面一样
    	outChannel.transferFrom(inChannel, 0, inChannel.size());
    
    	inChannel.close();
    	outChannel.close();
    }
    ```

    

* 分散（Scatter）与聚集（Gather）
    分散读取（Scattering Reads）：将一个通道中的数据分散到多个缓冲区中去
    聚集写入（Gathering Writes）：将多个缓冲区中的数据聚集到一个通道中

    ```java
    //分散读取
    public void test4() throws IOException {
    	RandomAccessFile in = new RandomAccessFile("D:\\1.txt", "rw");
    	ByteBuffer[] buffers = new ByteBuffer[]{ByteBuffer.allocate(500), ByteBuffer.allocate(1024)};
    	FileChannel inChannel = in.getChannel();
    	inChannel.read(buffers);
    	for (ByteBuffer buf : buffers) {
    		buf.flip();
    	}
    	RandomAccessFile out = new RandomAccessFile("D:\\2.txt", "rw");
    	FileChannel outChannel = out.getChannel();
    	outChannel.write(buffers);
    	
    	inChannel.close();
    	outChannel.close();
    	in.close();
    	out.close();
    }
    ```

    

* 字符集：`CharSet`
    编码：字符串-->字节数组
    解码：字节数组-->字符串

    ```java
    public void test5() throws CharacterCodingException {
    	Charset cst = Charset.forName("GBK");
    	CharsetEncoder encode = cst.newEncoder();
    	CharsetDecoder decode = cst.newDecoder();
    
    	CharBuffer buffer = CharBuffer.allocate(1024);
    	buffer.put("狗蛋");
    	buffer.flip();
    	//编码
    	ByteBuffer byteBuffer = encode.encode(buffer);
    	for (int i = 0; i < byteBuffer.limit(); i++) {
    		System.out.println(byteBuffer.get());
    	}
    
    	byteBuffer.flip();
    	CharBuffer charBuffer = decode.decode(byteBuffer);
    	System.out.println(charBuffer.toString());
    }
    ```

    



### 2. 缓冲区

发送给一个通道的所有数据都必须首先放到缓冲区中，同样地，从通道中读取的任何数据都要先读到缓冲区中。也就是说，不会直接对通道进行读写数据，而是要先经过缓冲区。

缓冲区实质上是一个数组，但它不仅仅是一个数组。缓冲区提供了对数据的结构化访问，而且还可以跟踪系统的读/写进程。

缓冲区包括以下类型：

- ByteBuffer
- CharBuffer
- ShortBuffer
- IntBuffer
- LongBuffer
- FloatBuffer
- DoubleBuffer

**直接缓冲区与非直接缓冲区** 

* 非直接缓冲区：通过allocate方法分配缓冲区，将缓冲区建立在JVM内存中
* 直接缓冲区：通过allocateDirect方法分配缓冲区，将缓冲区建立在操作系统的物理内存中（实现零拷贝），可以提高效率，但是和物理磁盘建立的引用不容易销毁，除非垃圾回收机制进行回收

## 缓冲区状态变量

- capacity：最大容量；一旦声明不能改变
- position：位置。表示缓冲区中正在操作的数据的位置。`0 <= mark <= position <= limit <= capacity`
- limit：界限。表示缓冲区中的可以操作数据的大小（界限）。即limit后面的位置是不能读写的
- mark：标记。表示记录当前的position的位置。通过reset方法恢复到mark的位置

状态变量的改变过程举例：

① 新建一个大小为 8 个字节的缓冲区，此时 position 为 0，而 limit = capacity = 8。capacity 变量不会改变，下面的讨论会忽略它。

![3](./assert/3.png)

② 从输入通道中读取 5 个字节数据写入缓冲区中，此时 position 为 5，limit 保持不变。

![4](./assert/4.png)

③ 在将缓冲区的数据写到输出通道之前，需要先调用 flip() 方法，这个方法将 limit 设置为当前 position，并将 position 设置为 0。

![5](./assert/5.png)

④ 从缓冲区中取 4 个字节到输出缓冲中，此时 position 设为 4。

![6](./assert/6.png)

⑤ 最后需要调用 clear() 方法来清空缓冲区，此时 position 和 limit 都被设置为最初位置。

![7](./assert/7.png)

```java
public void test1() {
	String str = "aaaa";
	ByteBuffer buffer = ByteBuffer.allocate(1024);
	//[pos=0 lim=1024 cap=1024]
	System.out.println("1. " + buffer);
	//往缓冲区中写数据
	buffer.put(str.getBytes());
	//[pos=4 lim=1024 cap=1024]
	System.out.println("2. " + buffer);
	//切换读写模式
	buffer.flip();
	//[pos=0 lim=4 cap=1024]
	System.out.println("3. " + buffer);
	byte[] bytes = new byte[buffer.limit()];
	//从缓冲区中取数据
	buffer.get(bytes);
	//[pos=4 lim=4 cap=1024]
	System.out.println("4. " + buffer);
	System.out.println(Arrays.toString(bytes));

	//使得buffer可重复读数据，pos又回到了0位置
	buffer.rewind();
	//[pos=0 lim=4 cap=1024]
	System.out.println("5. " + buffer);

	//清空缓冲区，但是缓冲区的数据依然存在
	buffer.clear();
	//[pos=0 lim=1024 cap=1024]
	System.out.println("6. " + buffer);
}
```



## 选择器

NIO 常常被叫做非阻塞 IO，主要是因为 NIO 在网络通信中的非阻塞特性被广泛使用。

NIO 实现了 IO 多路复用中的 Reactor 模型，一个线程 Thread 使用一个选择器 Selector 通过轮询的方式去监听多个通道 Channel 上的事件，从而让一个线程就可以处理多个事件。

通过配置监听的通道 Channel 为非阻塞，那么当 Channel 上的 IO 事件还未到达时，就不会进入阻塞状态一直等待，而是继续轮询其它 Channel，找到 IO 事件已经到达的 Channel 执行。

因为创建和切换线程的开销很大，因此使用一个线程来处理多个事件而不是一个线程处理一个事件，对于 IO 密集型的应用具有很好地性能。

应该注意的是，只有套接字 Channel 才能配置为非阻塞，而 FileChannel 不能，为 FileChannel 配置非阻塞也没有意义。

![8](./assert/8.jpg)

### 1. 创建选择器

```java
Selector selector = Selector.open();
```

### 2. 将通道注册到选择器上

```java
ServerSocketChannel ssChannel = ServerSocketChannel.open();
ssChannel.configureBlocking(false);
ssChannel.register(selector, SelectionKey.OP_ACCEPT);
```

通道必须配置为非阻塞模式，否则使用选择器就没有任何意义了，因为如果通道在某个事件上被阻塞，那么服务器就不能响应其它事件，必须等待这个事件处理完毕才能去处理其它事件，显然这和选择器的作用背道而驰。

在将通道注册到选择器上时，还需要指定要注册的具体事件，主要有以下几类：

- SelectionKey.OP_CONNECT 连接状态
- SelectionKey.OP_ACCEPT 接收状态
- SelectionKey.OP_READ  读状态
- SelectionKey.OP_WRITE 写状态

它们在 SelectionKey 的定义如下：

```java
public static final int OP_READ = 1 << 0;
public static final int OP_WRITE = 1 << 2;
public static final int OP_CONNECT = 1 << 3;
public static final int OP_ACCEPT = 1 << 4;
```

可以看出每个事件可以被当成一个位域，从而组成事件集整数。例如：

```java
int interestSet = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
```

### 3. 监听事件

```java
int num = selector.select();
```

使用 select() 来监听到达的事件，它会一直阻塞直到有至少一个事件到达。

### 4. 获取到达的事件

```java
Set<SelectionKey> keys = selector.selectedKeys();
Iterator<SelectionKey> keyIterator = keys.iterator();
while (keyIterator.hasNext()) {
    SelectionKey key = keyIterator.next();
    if (key.isAcceptable()) {
        // ...
    } else if (key.isReadable()) {
        // ...
    }
    keyIterator.remove();
}
```

### 5. 事件循环

因为一次 select() 调用不能处理完所有的事件，并且服务器端有可能需要一直监听事件，因此服务器端处理事件的代码一般会放在一个死循环内。

```java
while (true) {
    int num = selector.select();
    Set<SelectionKey> keys = selector.selectedKeys();
    Iterator<SelectionKey> keyIterator = keys.iterator();
    while (keyIterator.hasNext()) {
        SelectionKey key = keyIterator.next();
        if (key.isAcceptable()) {
            // ...
        } else if (key.isReadable()) {
            // ...
        }
        keyIterator.remove();
    }
}
```



## 阻塞式NIO

**使用NIO完成网络通信的三个核心**
1、通道:
java.nio.channels.Channel接口
* SelectableChannel
* SocketChannel
* ServerSocketChannel
* DatagramChannel
* Pipe.SinkChannel
* Pipe.SourceChannel

2、缓冲区
3、选择器：是SelectableChannel的多路复用器。用于监控SelectableChannel的IO状况

```java
public void client1() throws IOException {
	//获取通道
	SocketChannel cChannel = SocketChannel.open(new InetSocketAddress("localhost", 8888));
	//分配指定大小的缓冲区域
	ByteBuffer buf = ByteBuffer.allocate(1024);
	//读取本地文件发送
	FileChannel fileChannel = FileChannel.open(Paths.get("D:\\1.txt"), StandardOpenOption.READ);
	while (fileChannel.read(buf) != -1) {
		buf.flip();
		cChannel.write(buf);
		buf.clear();
	}
	//关闭通道
	cChannel.close();
	fileChannel.close();
}

public void server1() throws IOException {
	//获取通道
	ServerSocketChannel sChannel = ServerSocketChannel.open();
	//绑定链接
	sChannel.bind(new InetSocketAddress(8888));
	//获取客户端链接通道
	SocketChannel cChannel = sChannel.accept();
	//接收客户端发送过来的数据
	FileChannel fileChannel = FileChannel.open(Paths.get("D:\\2.txt"), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
	ByteBuffer buf = ByteBuffer.allocate(1024);
	while (cChannel.read(buf) != -1) {
		buf.flip();
		fileChannel.write(buf);
		buf.clear();
	}
	sChannel.close();
	cChannel.close();
	fileChannel.close();
}

/**
 * 上面例子是一个最简单的实现方式，下面这种方式需要服务端收到消息后
 * 对客户端进行反馈
 */
public void client2() throws IOException {
	SocketChannel cChannel = SocketChannel.open(new InetSocketAddress("localhost", 8888));
	ByteBuffer buf = ByteBuffer.allocate(1024);
	FileChannel fileChannel = FileChannel.open(Paths.get("D:\\1.txt"), StandardOpenOption.READ);
	while (fileChannel.read(buf) != -1) {
		buf.flip();
		cChannel.write(buf);
		buf.clear();
	}

	//接受服务端的反馈
	int len = 0;
	while ((len = cChannel.read(buf)) != -1) {
		buf.flip();
		System.out.println(new String(buf.array(), 0, len));
		buf.clear();
	}
	cChannel.close();
	fileChannel.close();
}

public void server2() throws IOException {
	ServerSocketChannel sChannel = ServerSocketChannel.open();
	sChannel.bind(new InetSocketAddress(8888));
	SocketChannel cChannel = sChannel.accept();
	FileChannel fileChannel = FileChannel.open(Paths.get("D:\\2.txt"), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
	ByteBuffer buf = ByteBuffer.allocate(1024);
	while (cChannel.read(buf) != -1) {
		buf.flip();
		fileChannel.write(buf);
		buf.clear();
	}

	buf.put("接收完毕!".getBytes());
	buf.flip();
	cChannel.write(buf);
	buf.clear();

	sChannel.close();
	cChannel.close();
	fileChannel.close();
}
```



## 非阻塞式NIO

```java
public void client1() throws IOException {
	//获取通道
	SocketChannel cChannel = SocketChannel.open(new InetSocketAddress("localhost", 8888));
	//切换为非阻塞模式
	cChannel.configureBlocking(false);
	ByteBuffer buf = ByteBuffer.allocate(1024);
	buf.put(LocalDateTime.now().toString().getBytes());
	buf.flip();
	cChannel.write(buf);
	buf.clear();
	cChannel.close();


ublic void server1() throws IOException {
	ServerSocketChannel sChannel = ServerSocketChannel.open();
	sChannel.configureBlocking(false);
	sChannel.bind(new InetSocketAddress(8888));
	//获取选择器
	Selector selector = Selector.open();
	//将通道注册到选择器上，第二个参数表示让选择器监听通道的什么状态，下面是指定为接收状态
	sChannel.register(selector, SelectionKey.OP_ACCEPT);
	//通多选择器轮询获取选择器上已经准备就绪的事件
	while (selector.select() > 0) {
		//获取到了所有选择器上的key迭代器
		Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
		while (iterator.hasNext()) {
			SelectionKey key = iterator.next();
			//针对不同的事件进行不同的处理
			if (key.isAcceptable()) {
				//获取客户端的连接
				SocketChannel cChannel = sChannel.accept();
				//客户端连接切换非阻塞模式
				cChannel.configureBlocking(false);
				//将该通道注册到选择器上
				cChannel.register(selector, SelectionKey.OP_READ);
			} else if (key.isReadable()) {
				//获取读就绪状态的通道
				SocketChannel readChannel = (SocketChannel) key.channel();
				//读数据
				ByteBuffer buf = ByteBuffer.allocate(1024);
				int len = 0;
				while ((len = readChannel.read(buf)) != -1) {
					buf.flip();
					readChannel.write(buf);
					buf.clear();
				}
			}
			//操作完之后一定要从选择器上将key删除
			iterator.remove();
		}
	}
	sChannel.close();
}
```





## 内存映射文件

内存映射文件 I/O 是一种读和写文件数据的方法，它可以比常规的基于流或者基于通道的 I/O 快得多。

向内存映射文件写入可能是危险的，只是改变数组的单个元素这样的简单操作，就可能会直接修改磁盘上的文件。修改数据与将数据保存到磁盘是没有分开的。

下面代码行将文件的前 1024 个字节映射到内存中，map() 方法返回一个 MappedByteBuffer，它是 ByteBuffer 的子类。因此，可以像使用其他任何 ByteBuffer 一样使用新映射的缓冲区，操作系统会在需要时负责执行映射。

```java
MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, 1024);
```

## 对比

NIO 与普通 I/O 的区别主要有以下两点：

- NIO 是非阻塞的；
- NIO 面向块，I/O 面向流。



# 八、AIO

参考：

```http
https://juejin.im/entry/583ec2e3128fe1006bfa6c83
https://blog.csdn.net/anxpp/article/details/51512200
https://colobu.com/2014/11/13/java-aio-introduction/
```



jdk7中新增了一些与文件(网络)I/O相关的一些api。这些API被称为NIO.2，或称为AIO(Asynchronous I/O)。AIO最大的一个特性就是异步能力，这种能力对socket与文件I/O都起作用。AIO其实是一种在读写操作结束之前允许进行其他操作的I/O处理。AIO是对JDK1.4中提出的同步非阻塞I/O(NIO)的进一步增强。

jdk7主要增加了三个新的异步通道:

* AsynchronousFileChannel: 用于文件异步读写；
* AsynchronousSocketChannel: 客户端异步socket；
* AsynchronousServerSocketChannel: 服务器异步socket。

因为AIO的实施需充分调用OS参与，IO需要操作系统支持、并发也同样需要操作系统的支持，所以性能方面不同操作系统差异会比较明显。注意：在linux上，AIO并不一定比NIO快，这也是为什么Netty是基于NIO实现，而不是基于AIO实现。

在不同的操作系统上,AIO由不同的技术实现。
Windows上是使用完成接口(`IOCP`)实现,可以参看`WindowsAsynchronousServerSocketChannelImpl`,
其它平台上使用aio调用`UnixAsynchronousServerSocketChannelImpl, UnixAsynchronousSocketChannelImpl, SolarisAsynchronousChannelProvider`



异步channel API提供了两种方式监控/控制异步操作(connect,accept, read，write等)。

* 第一种方式是返回java.util.concurrent.Future对象， 检查Future的状态可以得到操作是否完成还是失败，还是进行中， future.get阻塞当前进程。
* 第二种方式为操作提供一个回调参数java.nio.channels.CompletionHandler，这个回调类包含completed,failed两个方法。

```java
// accept使用了回调的方式， 而发送数据使用了future的方式。
public class Server {
	private static Charset charset = Charset.forName("US-ASCII");
    private static CharsetEncoder encoder = charset.newEncoder();
	
	public static void main(String[] args) throws Exception {
		AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(4));
		AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(group).bind(new InetSocketAddress("0.0.0.0", 8013));
		server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
			@Override
			public void completed(AsynchronousSocketChannel result, Void attachment) {
				server.accept(null, this); // 接受下一个连接
				try {
					 String now = new Date().toString();
					 ByteBuffer buffer = encoder.encode(CharBuffer.wrap(now + "\r\n"));
					//result.write(buffer, null, new CompletionHandler<Integer,Void>(){...}); //callback or
					Future<Integer> f = result.write(buffer);
					f.get();
					System.out.println("sent to client: " + now);
					result.close();
				} catch (IOException | InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
			@Override
			public void failed(Throwable exc, Void attachment) {
				exc.printStackTrace();
			}
		});
		group.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
	}
}

//connect使用了future方式，而接收数据使用了回调的方式。
public class Client {
	public static void main(String[] args) throws Exception {
		AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
		Future<Void> future = client.connect(new InetSocketAddress("127.0.0.1", 8013));
		future.get();
		
		ByteBuffer buffer = ByteBuffer.allocate(100);
		client.read(buffer, null, new CompletionHandler<Integer, Void>() {
			@Override
			public void completed(Integer result, Void attachment) {
				System.out.println("client received: " + new String(buffer.array()));
			}
			@Override
			public void failed(Throwable exc, Void attachment) {
				exc.printStackTrace();
				try {
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		});
		Thread.sleep(10000);
	}
}
```

src包中的例子更为复杂一些。



## io.netty.buffer和java.nio.ByteBuffer的区别

ByteBuffer主要有两个继承的类分别是：HeapByteBuffer和MappedByteBuffer。他们的不同之处在于

HeapByteBuffer会在JVM的堆上分配内存资源，而MappedByteBuffer的资源则会由JVM之外的操作系统内核来分

配。DirectByteBuffer继承了MappedByteBuffer，采用了直接内存映射的方式，将文件直接映射到虚拟内存，同

时减少在内核缓冲区和用户缓冲区之间的调用，尤其在处理大文件方面有很大的性能优势。但是在使用内存映射的

时候会造成文件句柄一直被占用而无法删除的情况，网上也有很多介绍。

Netty中使用ChannelBuffer来处理读写，之所以废弃ByteBuffer，官方说法是ChannelBuffer简单易用并且有性

能方面的优势。在ChannelBuffer中使用ByteBuffer或者byte[]来存储数据。同样的，ChannelBuffer也提供了几

个标记来控制读写并以此取代ByteBuffer的position和limit，分别是：

0 <= readerIndex <= writerIndex <= capacity，同时也有类似于mark的markedReaderIndex和

markedWriterIndex。当写入buffer时，writerIndex增加，从buffer中读取数据时readerIndex增加，而不能超过

writerIndex。有了这两个变量后，就不用每次写入buffer后调用flip()方法，方便了很多。



## Netty的零拷贝(zero copy)

Netty的“零拷贝”主要体现在如下三个方面：

* 1) Netty的接收和发送ByteBuffer采用DIRECT BUFFERS，使用堆外直接内存进行Socket读写，不需要进行字节缓冲区的二次拷贝。如果使用传统的堆内存（HEAP BUFFERS）进行Socket读写，JVM会将堆内存Buffer拷贝一份到直接内存中，然后才写入Socket中。相比于堆外直接内存，消息在发送过程中多了一次缓冲区的内存拷贝。
* 2) Netty提供了组合Buffer对象，可以聚合多个ByteBuffer对象，用户可以像操作一个Buffer那样方便的对组合Buffer进行操作，避免了传统通过内存拷贝的方式将几个小Buffer合并成一个大的Buffer。
* 3) Netty的文件传输采用了transferTo方法，它可以直接将文件缓冲区的数据发送到目标Channel，避免了传统通过循环write方式导致的内存拷贝问题。









