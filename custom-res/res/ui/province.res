Haven Resource 1 src   Province.java /* Preprocessed source code */
package haven.res.ui.province;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import haven.PUtils.*;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.nio.ByteBuffer;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;

/* >wdg: Province */
public class Province extends Widget {
    public static final Coord2d place = new Coord2d(0.5, 0.2);
    /*
    public static final Text.Furnace provf = new BlurFurn(new Text.Foundry(Text.fraktur, 50).aa(true), UI.scale(5), UI.scale(4), Color.BLACK);
    public static final Text.Furnace ownf = new BlurFurn(new Text.Foundry(Text.fraktur, 25).aa(true), UI.scale(5), UI.scale(4), Color.BLACK);
    */
    public static final Text.Furnace provf = new BlurFurn(new TexFurn(new Text.Foundry(Text.fraktur, 50).aa(true), Window.ctex), UI.scale(5), UI.scale(4), new Color(96, 48, 0));
    public static final Text.Furnace ownf = new BlurFurn(new TexFurn(new Text.Foundry(Text.fraktur, 25).aa(true), Window.ctex), UI.scale(3), UI.scale(2), new Color(96, 48, 0));
    public String name, owner;
    public Integer ownerid;
    public boolean old;
    private boolean drawn = false;
    private Tex img;
    private double t = 0, wait = 0;
    public static final Texture2D.Sampler2D cloud;

    private static double nval(SNoise3 rnd, double x, double y) {
	return((rnd.getr(-1, 1, 1.0 /  4.0, x, y, 4219) * 0.5714286) +
	       (rnd.getr(-1, 1, 1.0 / 16.0, x, y, 4219) * 0.2857143) +
	       (rnd.getr(-1 ,1, 1.0 / 64.0, x, y, 4219) * 0.1428571));
    }

    static {
	final Coord sz = new Coord(256, 256);
	byte[] buf = new byte[sz.x * sz.y];
	SNoise3 rnd = new SNoise3();
	for(int y = 0, o = 0; y < sz.y; y++) {
	    double Y = (double)y / sz.y;
	    for(int x = 0; x < sz.x; x++) {
		double X = (double)x / sz.x;
		double a =
		    (((nval(rnd, X - 0, Y - 0) * (1 - X)) + (nval(rnd, X - 1, Y - 0) * X)) * (1 - Y)) +
		    (((nval(rnd, X - 0, Y - 1) * (1 - X)) + (nval(rnd, X - 1, Y - 1) * X)) * Y);
		buf[o++] = (byte)Math.round(127 * Utils.clip(a, -1, 1));
	    }
	}
	/*
	try(OutputStream barda = Files.newOutputStream(Utils.path("/tmp/barda.pgm"))) {
	    barda.write("P5 256 256 255\n".getBytes(Utils.ascii));
	    for(int i = 0; i < buf.length; i++)
		barda.write((byte)(buf[i] + 127));
	} catch(IOException e) {
	    e.printStackTrace();
	}
	*/
	Texture2D tex = new Texture2D(sz, DataBuffer.Usage.STATIC, new VectorFormat(1, NumberFormat.SNORM8),
				      (img, env) -> {
					  if(img.level != 0)
					      return(null);
					  FillBuffer fbuf = env.fillbuf(img);
					  fbuf.pull(ByteBuffer.wrap(buf));
					  return(fbuf);
				      });
	cloud = new Texture2D.Sampler2D(tex);
    }

    public Province(String name, boolean old) {
	this.name = name;
	this.old = old;
    }

    public static Widget mkwidget(UI ui, Object... args) {
	int a = 0;
	String name = (String)args[a++];
	boolean old = ((Integer)args[a++]) == 0;
	Province ret = new Province(name, old);
	Integer ownerid = (Integer)args[a++];
	if(ownerid != null) {
	    ret.ownerid = ownerid;
	    ret.owner = (String)args[a++];
	}
	return(ret);
    }

    public Tex render() {
	BufferedImage[] imgs = {null, null};
	imgs[0] = provf.render("Entering " + name).img;
	if(ownerid != null)
	    imgs[1] = ownf.render("in " + owner).img;
	int n = 0, w = 0;
	for(int i = 0; i < imgs.length; i++) {
	    if(imgs[i] != null) {
		imgs[n++] = imgs[i];
		w = Math.max(w, imgs[i].getWidth());
	    }
	}
	if(n == 1)
	    return(new TexI(imgs[0]));
	CompImage ret = new CompImage();
	for(int i = 0; i < n; i++)
	    ret.add(imgs[i], new Coord((w - imgs[i].getWidth()) / 2, ret.sz.y));
	return(new TexI(ret.compose()));
    }

    class Draw extends RUtils.AdHoc {
	final float in, out;
	Draw(float in, float out) {super(code); this.in = in; this.out = out;}
	Province wdg() {return(Province.this);}
    }
    public static final Uniform u_in = new Uniform(FLOAT, p -> ((Draw)p.get(RUtils.adhoc)).in, RUtils.adhoc);
    public static final Uniform u_out = new Uniform(FLOAT, p -> ((Draw)p.get(RUtils.adhoc)).out, RUtils.adhoc);
    public static final Uniform u_sz = new Uniform(VEC2, p -> ((Draw)p.get(RUtils.adhoc)).wdg().sz, RUtils.adhoc);
    public static final Uniform ctex = new Uniform(SAMPLER2D, p -> cloud);
    public static final Function wipe = new Function.Def(VEC4) {{
	Expression in = param(PDir.IN, VEC4).ref();
	Expression pc = code.local(VEC2, mul(Tex2D.rtexcoord.ref(), u_sz.ref())).ref();
	Expression texc = code.local(VEC2, mul(pc, vec2(1.0 / cloud.tex.w, 1.0 / cloud.tex.h))).ref();
	Expression dist = code.local(FLOAT, pick(Tex2D.rtexcoord.ref(), "x")).ref();
	code.add(new Return(vec4(pick(in, "rgb"),
				 mul(pick(in, "a"), clamp(Cons.add(l(-1.0),
								   mul(clamp(sub(mul(u_in.ref(), l(2.0)), dist), l(0.0), l(1.0)), l(3.0)),
								   mul(clamp(sub(mul(u_out.ref(), l(2.0)), dist), l(0.0), l(1.0)), l(-3.0)),
								   mul(pick(texture2D(ctex.ref(), Cons.add(texc, mul(FrameInfo.time(), vec2(0.2, 0.1)))), "r"), l(0.5)),
								   mul(pick(texture2D(ctex.ref(), Cons.add(texc, mul(FrameInfo.time(), vec2(-0.15, -0.2)))), "r"), l(0.5))),
							  l(0.0), l(1.0))))));
    }};
    public static final ShaderMacro code = prog -> FragColor.fragcol(prog.fctx).mod(wipe::call, 10);

    public void draw(GOut g) {
	drawn = true;
	Draw st = null;
	if(t < 4.0)
	    st = new Draw((float)(t / 4.0), 0);
	else if(t < 6.0)
	    st = new Draw(1, (float)((t - 4.0) * 0.5));
	if(st != null) {
	    g.usestate(st);
	    g.image(img, Coord.z);
	}
    }

    public void presize() {
	resize(img.sz());
	move(parent.sz.sub(this.sz).mul(place).round());
    }

    private void previnit() {
	Province pd = null, pw = null;
	for(Widget ch : parent.children()) {
	    if((ch instanceof Province) && (ch != this)) {
		Province prev = (Province)ch;
		if(prev.t == 0)
		    pw = prev;
		else
		    pd = prev;
	    }
	}
	if(pw != null)
	    ui.destroy(pw);
	if(pd != null) {
	    if(pd.t < 4.0) {
		this.t = pd.t;
		ui.destroy(pd);
	    } else {
		wait = 10 - pd.t;
	    }
	}
    }

    protected void added() {
	img = render();
	previnit();
	super.added();
	presize();
    }

    public void tick(double dt) {
	super.tick(dt);
	if(old) {
	    ui.destroy(this);
	    return;
	}
	if(drawn) {
	    if(wait > 0) {
		wait -= dt;
	    } else {
		t += dt;
		if(t >= 10)
		    ui.destroy(this);
	    }
	}
    }
}
code �  haven.res.ui.province.Province$Draw ����   4 '	  	  
  	  	     in F out this$0  Lhaven/res/ui/province/Province; <init> %(Lhaven/res/ui/province/Province;FF)V Code LineNumberTable wdg "()Lhaven/res/ui/province/Province; 
SourceFile Province.java   ! " #  $  	 
 	 #haven/res/ui/province/Province$Draw Draw InnerClasses % haven/RUtils$AdHoc AdHoc haven/res/ui/province/Province code Lhaven/render/sl/ShaderMacro;  (Lhaven/render/sl/ShaderMacro;)V haven/RUtils province.cjava          	    
 	                /     *+� *� � *$� *%� �           n              *� �           o      &               	code   haven.res.ui.province.Province$1 ����   4 �
 = F	 G H	 I J
 < K
 L M	 < N	 I O P	 Q R
 S T	 E U
 V T
 W X
 Y Z
 [ M	 E \	 ] ^ _	  `	  a
 W b	 I c d
 W e f g
 W h i��      
 W j	 E k@       
 W l
 W m@      	 E n�      	 E o
 p q?ə�����?�������
 W r
 W s t?�      ��333333�ə�����
 W u
  v
 Y w x { <init> (Lhaven/render/sl/Type;)V Code LineNumberTable 
SourceFile Province.java EnclosingMethod } > ? ~ � � � � � � � � � � � � � � haven/render/sl/Expression � � � � � � � � � � � � � � � � � � � � � haven/render/Texture2D � � � � � � � � x � � haven/render/sl/Return rgb � � a � � � � � � � � � � � � � � � � � � � r � � > � � �  haven/res/ui/province/Province$1 InnerClasses � haven/render/sl/Function$Def Def haven/res/ui/province/Province haven/render/sl/Function$PDir PDir IN Lhaven/render/sl/Function$PDir; haven/render/sl/Type VEC4 Lhaven/render/sl/Type; param 	Parameter [(Lhaven/render/sl/Function$PDir;Lhaven/render/sl/Type;)Lhaven/render/sl/Function$Parameter; "haven/render/sl/Function$Parameter ref � Ref  ()Lhaven/render/sl/Variable$Ref; code Lhaven/render/sl/Block; VEC2 haven/render/Tex2D 	rtexcoord Lhaven/render/sl/AutoVarying; haven/render/sl/AutoVarying � Global � '()Lhaven/render/sl/Variable$Global$Ref; u_sz Lhaven/render/sl/Uniform; haven/render/sl/Uniform haven/render/sl/Cons mul 4([Lhaven/render/sl/Expression;)Lhaven/render/sl/Mul; haven/render/sl/Block local Local Q(Lhaven/render/sl/Type;Lhaven/render/sl/Expression;)Lhaven/render/sl/Block$Local; haven/render/sl/Block$Local cloud 	Sampler2D "Lhaven/render/Texture2D$Sampler2D;  haven/render/Texture2D$Sampler2D tex Lhaven/render/Texture; w I h vec2  (DD)Lhaven/render/sl/Expression; FLOAT pick C(Lhaven/render/sl/LValue;Ljava/lang/String;)Lhaven/render/sl/LPick; F(Lhaven/render/sl/Expression;Ljava/lang/String;)Lhaven/render/sl/Pick; l !(D)Lhaven/render/sl/FloatLiteral; u_in sub � Sub U(Lhaven/render/sl/Expression;Lhaven/render/sl/Expression;)Lhaven/render/sl/BinOp$Sub; clamp r(Lhaven/render/sl/Expression;Lhaven/render/sl/Expression;Lhaven/render/sl/Expression;)Lhaven/render/sl/Expression; u_out ctex haven/render/FrameInfo time ()Lhaven/render/sl/Expression; add 4([Lhaven/render/sl/Expression;)Lhaven/render/sl/Add; 	texture2D V(Lhaven/render/sl/Expression;Lhaven/render/sl/Expression;)Lhaven/render/sl/Expression; vec4 9([Lhaven/render/sl/Expression;)Lhaven/render/sl/Vec4Cons; (Lhaven/render/sl/Expression;)V (Lhaven/render/sl/Statement;)V haven/render/sl/Function � haven/render/sl/Variable$Ref haven/render/sl/Variable$Global #haven/render/sl/Variable$Global$Ref � haven/render/sl/BinOp$Sub haven/render/sl/Variable haven/render/sl/BinOp province.cjava 0 < =        > ?  @  L    �*+� *� � � � M*� � � Y� 	� 
SY� � S� � � N*� � � Y-SY� � � � �o� � � � �o� S� � � :*� � � 	� 
� � � :*� � Y� Y,� SY� Y,� SY� Y � SY� Y� Y�  � SY !� S� � #� � � $SY %� S� SY� Y� Y� '� SY !� S� � #� � � $SY (� S� SY� Y� *� � YSY� Y� +SY , .� S� S� 0� 12� SY 3� S� SY� Y� *� � YSY� Y� +SY 5 7� S� S� 0� 12� SY 3� S� S� 0� � � $S� S� 9� :� ;�    A   B    u  v  w : x x y � z � { � | }N ~� � {� �� {� z� �  B    � y   R 
 <      = z | 	 G z @ L z � 	 � � �  � � � 	 � � �  [ Y �  ]  � 	 � � � 	 D    E  code �&  haven.res.ui.province.Province ����   4S��      ?�      @�{     
 ~ �?�I$���X?�      ?�I$���X?�      ?�I$6@-�
 P �	  �	  �	  �	  �	  � � �
  � �
  �	  �	  � �	  
 ! �
 !
 !
 �		 	
 


 ,
 . �	 .	 0
 0
 .
 .@      
 8@      ?�      
	 	 0

 	  	 P	 
 0!	 "
 0#
 m$
 %
 P&'()*)+,	 -
./@$      
 0
 1
 P2
 3
 P4	56
78	 9
:;  A
BCD
EF	 G	HIJK
 8L	 8M
NO	 8P	 �QRS
TUVWX?ə�����
 mY[]_	a
 sb
 sc	de
 rf
.gh
 zi
 qj	 0kl
 ~ �
 m@_�     
no
pq	rst	uv
 �w {
 �|}
 �~	�� ��
 ��	 � �	 �	�� �	 �	�� �	 ��	��
 �� �	 � Draw InnerClasses place Lhaven/Coord2d; provf� Furnace Lhaven/Text$Furnace; ownf name Ljava/lang/String; owner ownerid Ljava/lang/Integer; old Z drawn img Lhaven/Tex; t D wait cloud 	Sampler2D "Lhaven/render/Texture2D$Sampler2D; u_in Lhaven/render/sl/Uniform; u_out u_sz ctex wipe Lhaven/render/sl/Function; code Lhaven/render/sl/ShaderMacro; nval (Lhaven/SNoise3;DD)D Code LineNumberTable <init> (Ljava/lang/String;Z)V mkwidget -(Lhaven/UI;[Ljava/lang/Object;)Lhaven/Widget; StackMapTable � � � render ()Lhaven/Tex;� draw (Lhaven/GOut;)V presize ()V previnit�, added tick (D)V lambda$static$6 #(Lhaven/render/sl/ProgramContext;)V lambda$null$5 T(Lhaven/render/sl/Function;Lhaven/render/sl/Expression;)Lhaven/render/sl/Expression; lambda$static$4 '(Lhaven/render/Pipe;)Ljava/lang/Object; lambda$static$3 lambda$static$2 lambda$static$1 lambda$static$0� Image S([BLhaven/render/Texture$Image;Lhaven/render/Environment;)Lhaven/render/FillBuffer; <clinit>�l 
SourceFile Province.java�� � � � � � � � � � � � � java/lang/String java/lang/Integer�� haven/res/ui/province/Province � � � � � � java/awt/image/BufferedImage � � java/lang/StringBuilder 	Entering ���� ��� �� � � in ����� 
haven/TexI �� haven/CompImage haven/Coord���� ������ #haven/res/ui/province/Province$Draw ����� � �������������� � ����������������� haven/Widget����� � � � � � � � � � ������� � ���� BootstrapMethods��������� haven/render/sl/Expression��� � �������������������  haven/Coord2d �	 haven/PUtils$BlurFurn BlurFurn haven/PUtils$TexFurn TexFurn haven/Text$Foundry Foundry
 � �� � java/awt/Color � �� haven/SNoise3 � �� haven/render/Texture2D haven/render/VectorFormat ! �"#$%&) �*  haven/render/Texture2D$Sampler2D �+ haven/render/sl/Uniform,-./ ��01 haven/render/State$Slot Slot �2 � �3 � �4.5 � �6.7 � �  haven/res/ui/province/Province$18. �9 �:;< � � haven/Text$Furnace [Ljava/awt/image/BufferedImage; java/util/Iterator= haven/render/Texture$Image [B getr 	(DDDDDD)D intValue ()I append -(Ljava/lang/String;)Ljava/lang/StringBuilder; toString ()Ljava/lang/String;  (Ljava/lang/String;)Lhaven/Text; 
haven/Text Ljava/awt/image/BufferedImage; getWidth java/lang/Math max (II)I !(Ljava/awt/image/BufferedImage;)V sz Lhaven/Coord; y I (II)V add >(Ljava/awt/image/BufferedImage;Lhaven/Coord;)Lhaven/CompImage; compose  ()Ljava/awt/image/BufferedImage; %(Lhaven/res/ui/province/Province;FF)V 
haven/GOut usestate (Lhaven/render/State;)V z image (Lhaven/Tex;Lhaven/Coord;)V 	haven/Tex ()Lhaven/Coord; resize (Lhaven/Coord;)V parent Lhaven/Widget; sub (Lhaven/Coord;)Lhaven/Coord; mul  (Lhaven/Coord2d;)Lhaven/Coord2d; round move children ()Ljava/util/List; java/util/List iterator ()Ljava/util/Iterator; hasNext ()Z next ()Ljava/lang/Object; ui 
Lhaven/UI; haven/UI destroy (Lhaven/Widget;)V haven/render/sl/ProgramContext fctx !Lhaven/render/sl/FragmentContext; haven/render/FragColor fragcol Value C(Lhaven/render/sl/FragmentContext;)Lhaven/render/sl/ValBlock$Value; java/lang/Object getClass ()Ljava/lang/Class;
>? &(Ljava/lang/Object;)Ljava/lang/Object;
 @ :(Lhaven/render/sl/Expression;)Lhaven/render/sl/Expression; apply >(Lhaven/render/sl/Function;)Ljava/util/function/UnaryOperator;A haven/render/sl/ValBlock$Value mod &(Ljava/util/function/UnaryOperator;I)V haven/render/sl/Function call ;([Lhaven/render/sl/Expression;)Lhaven/render/sl/Expression; haven/RUtils adhoc Lhaven/render/State$Slot; haven/render/Pipe get /(Lhaven/render/State$Slot;)Lhaven/render/State; wdg "()Lhaven/res/ui/province/Province; out F java/lang/Float valueOf (F)Ljava/lang/Float; in level haven/render/Environment fillbuf 4(Lhaven/render/DataBuffer;)Lhaven/render/FillBuffer; java/nio/ByteBuffer wrap ([B)Ljava/nio/ByteBuffer; haven/render/FillBuffer pull (Ljava/nio/ByteBuffer;)V (DD)V haven/PUtils fraktur Ljava/awt/Font; (Ljava/awt/Font;I)V aa (Z)Lhaven/Text$Foundry; haven/Window 5(Lhaven/Text$Furnace;Ljava/awt/image/BufferedImage;)V scale (I)I (III)V )(Lhaven/Text$Furnace;IILjava/awt/Color;)V x haven/Utils clip (DDD)D (D)JB haven/render/DataBuffer$Usage Usage STATIC Lhaven/render/DataBuffer$Usage; haven/render/NumberFormat SNORM8 Lhaven/render/NumberFormat; (ILhaven/render/NumberFormat;)V N(Lhaven/render/DataBuffer;Lhaven/render/Environment;)Lhaven/render/FillBuffer;
 C Q(Lhaven/render/Texture$Image;Lhaven/render/Environment;)Lhaven/render/FillBuffer; fillD Filler $([B)Lhaven/render/DataBuffer$Filler; j(Lhaven/Coord;Lhaven/render/DataBuffer$Usage;Lhaven/render/VectorFormat;Lhaven/render/DataBuffer$Filler;)V (Lhaven/render/Texture2D;)V haven/render/sl/Type FLOAT Lhaven/render/sl/Type;
 E ()Ljava/util/function/Function; haven/render/State P(Lhaven/render/sl/Type;Ljava/util/function/Function;[Lhaven/render/State$Slot;)V
 F VEC2
 G 	SAMPLER2D
 H VEC4 (Lhaven/render/sl/Type;)V
 I modify ()Lhaven/render/sl/ShaderMacro; haven/render/TextureJKN � � haven/render/sl/ValBlock haven/render/DataBuffer � � haven/render/DataBuffer$Filler � � � � � � � � � � "java/lang/invoke/LambdaMetafactory metafactoryP Lookup �(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;Q %java/lang/invoke/MethodHandles$Lookup java/lang/invoke/MethodHandles province.cjava !  P     � �    � �    � �    � �    � �    � �    � �    � �    � �    � �    � �    � �    � �    � �    � �    � �    � �    � �    
 � �  �   c     ?*  ') �  k*  
') �  kc*  ') �  kc�    �        !   6 ! >   � �  �   J     *� *� *� *� *+� *� �    �       F   	   G  H  I � � �  �   �     V=+�2� N+�2� � � � 6� Y-� :+�2� :� � +�2� � �    �    �  �@� 2 � � �   & 	   L  M  N " O . P 9 Q > R E S S U  � �  �  ]     �� YSYSL+�  � !Y� "#� $*� � $� %� &� 'S*� � %+� (� !Y� ")� $*� � $� %� &� 'S=>6+�� &+2� +�+2S+2� *� +>����� � ,Y+2� -�� .Y� /:6� -+2� 0Y+2� *dl� 1� 2� 3� 4W���ӻ ,Y� 5� -�    �    � X �� #� �  �� / �   F    Y  Z / [ 6 \ X ] \ ^ f _ m ` w a � ^ � d � e � f � g � h � g � i  � �  �   �     _*� M*�  6�� � 8Y**�  6o�� 9M� %*�  :�� � 8Y**�  6g <k�� 9M,� +,� >+*� ?� @� A�    �   
 � ( �! �   * 
   �  �  �  � ( � 3 � J � N � S � ^ �  � �  �   I     )**� ?� B � C**� D� E*� F� G� H� I� J� K�    �       �  � ( �  � �  �       �LM*� D� L� M N-� N � 9-� O � P:� � #*� � :� �� 	M� L���,� *� Q,� R+� -+�  6�� *+� � *� Q+� R� * S+� g� �    �     �  � � �� 8 � �� � ! �   B    �  � % � 3 � : � D � J � M � P � T � \ � ` � k � s � ~ � � �  � �  �   =     **� U� ?*� V*� W*� X�    �       �  �  �  �  �  � �  �   �     P*'� Y*� � *� Q*� R�*� � 6*� �� *Y� 'g� �  *Y� 'c� *�  S�� *� Q*� R�    �     �   .    �  �  �  �  �  � % � 2 � < � G � O �
 � �  �   2     *� Z� [� \Y� ]W� ^  
� _�    �       �
 � �  �   %     *� `Y+S� a�    �       �
 � �  �        � b�    �       t
 � �  �   +     *� c� d � 8� e� F�    �       s
 � �  �   +     *� c� d � 8� f� g�    �       r
 � �  �   +     *� c� d � 8� h� g�    �       q
 � �  �   N     +� i� �,+� j N-*� k� l -�    �    	 �       =  > 	 ?  @  A  � �  �  �    � mY < n� p� H� qY� rY� sY� t2� u� v� w� x� y� y� zY`0� {� |�  � qY� rY� sY� t� u� v� w� x� y� y� zY`0� {� |� (� 0Y  � 3K*� }*� 2h�L� ~Y� M>6*� 2� ��*� 2�o96*� }� z�*� }�o9,gg� �gk,gg� �kcgk,gg� �gk,gg� �kckc9
+� �
 � �k� ���T�������h� �Y*� �� �Y� �� �+� �  � �N� �Y-� �� b� �Y� �� �  � �Y� cS� �� �� �Y� �� �  � �Y� cS� �� �� �Y� �� �  � �Y� cS� �� �� �Y� �� �  � �� �� �� �Y� �� �� \� �  � ��    �    � �  � � �  � � �  �   b       H  � % � & � ' � ( � ) � * � + � , � - � . /6 *< (B ;_ Cj q� r� s� t� u� � <   H = >?@= xyz= >��= >��= >��= >��= ��� �   R �   j  8  �   �      � �	 � � � 	 �� � 	 qZ\ 	 rZ^ 	 s` 	 ��� 	B��r@'(	LOM codeentry &   wdg haven.res.ui.province.Province   