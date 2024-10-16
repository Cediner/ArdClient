Haven Resource 1 src �  ItemTex.java /* Preprocessed source code */
/* $use: lib/mapres */

package haven.res.lib.itemtex;

import haven.*;
import haven.render.*;
import haven.res.lib.mapres.ResourceMap;
import java.util.*;
import java.awt.image.BufferedImage;

public class ItemTex {
    public static class Icon implements GSprite.Owner, Resource.Resolver {
	public final Resource res;
	final Resource.Resolver pool;

	Icon(Resource res, Resource.Resolver pool) {
	    this.res = res;
	    this.pool = pool;
	}

	public Indir<Resource> getres(int id) {
	    return(null);
	}

	static final ClassResolver<Icon> rsv = new ClassResolver<Icon>()
	    .add(Resource.Resolver.class, ico -> ico.pool);
	public <C> C context(Class<C> cl) {return(rsv.context(cl, this));}
	public Resource getres() {return(res);}
	public Random mkrandoom() {return(new Random());}
    }
    
    public static GSprite mkspr(OwnerContext owner, Message sdt) {
	int resid = sdt.uint16();
	Message isdt = Message.nil;
	if((resid & 0x8000) != 0) {
	    resid &= ~0x8000;
	    isdt = new MessageBuf(sdt.bytes(sdt.uint8()));
	}
	Resource ires = owner.context(Resource.Resolver.class).getres(resid).get();
	GSprite.Owner ctx = new Icon(ires, new ResourceMap(owner.context(Resource.Resolver.class), sdt));
	return(GSprite.create(ctx, ires, isdt));
    }

    public static BufferedImage sprimg(GSprite spr) {
	if(spr instanceof GSprite.ImageSprite)
	    return(((GSprite.ImageSprite)spr).image());
	return(spr.owner.getres().layer(Resource.imgc).img);
    }

    public static final Map<MessageBuf, BufferedImage> made = new CacheMap<>();
    public static BufferedImage create(OwnerContext owner, Message osdt) {
	MessageBuf copy = new MessageBuf(osdt.bytes());
	synchronized(made) {
	    BufferedImage ret = made.get(copy);
	    if(ret == null)
		made.put(copy, ret = sprimg(mkspr(owner, copy.clone())));
	    return(ret);
	}
    }

    public static BufferedImage fixsz(BufferedImage img) {
	Coord sz = PUtils.imgsz(img);
	int msz = Math.max(sz.x, sz.y);
	int nsz = Math.max((int)Math.round(Math.pow(2, Math.round(Math.log(sz.x) / Math.log(2)))),
			   (int)Math.round(Math.pow(2, Math.round(Math.log(sz.y) / Math.log(2)))));
	BufferedImage ret = TexI.mkbuf(new Coord(nsz, nsz));
	java.awt.Graphics g = ret.getGraphics();
	int w = (sz.x * nsz) / msz, h = (sz.y * nsz) / msz;
	g.drawImage(img, (nsz - w) / 2, (nsz - h) / 2, (nsz + w) / 2, (nsz + h) / 2, 0, 0, sz.x, sz.y, null);
	g.dispose();
	return(ret);
    }

    public static final Map<BufferedImage, TexL> fixed = new CacheMap<>();
    public static TexL fixup(final BufferedImage img) {
	TexL tex;
	synchronized(fixed) {
	    tex = fixed.get(img);
	    if(tex == null) {
		BufferedImage fimg = img;
		Coord sz = PUtils.imgsz(fimg);
		if((sz.x != sz.y) || (sz.x != Tex.nextp2(sz.x)) || (sz.y != Tex.nextp2(sz.y))) {
		    fimg = fixsz(fimg);
		    sz = PUtils.imgsz(fimg);
		}
		final BufferedImage timg = fimg;
		tex = new TexL(sz) {
			public BufferedImage fill() {
			    return(timg);
			}
		    };
		tex.mipmap(Mipmapper.dav);
		tex.img.magfilter(Texture.Filter.LINEAR).minfilter(Texture.Filter.LINEAR).mipfilter(Texture.Filter.LINEAR);
		fixed.put(img, tex);
	    }
	}
	return(tex);
    }
}
code �  haven.res.lib.itemtex.ItemTex$Icon ����   4 ^
  /	  0	  1	  2
  3 4
  / 6
  / 8   >
  ? A B D res Lhaven/Resource; pool Resolver InnerClasses Lhaven/Resource$Resolver; rsv ClassResolver "Lhaven/OwnerContext$ClassResolver; 	Signature Icon HLhaven/OwnerContext$ClassResolver<Lhaven/res/lib/itemtex/ItemTex$Icon;>; <init> ,(Lhaven/Resource;Lhaven/Resource$Resolver;)V Code LineNumberTable getres (I)Lhaven/Indir; "(I)Lhaven/Indir<Lhaven/Resource;>; context %(Ljava/lang/Class;)Ljava/lang/Object; 1<C:Ljava/lang/Object;>(Ljava/lang/Class<TC;>;)TC; ()Lhaven/Resource; 	mkrandoom ()Ljava/util/Random; lambda$static$0 ?(Lhaven/res/lib/itemtex/ItemTex$Icon;)Lhaven/Resource$Resolver; <clinit> ()V 
SourceFile ItemTex.java  ,       # F java/util/Random G  haven/OwnerContext$ClassResolver H haven/Resource$Resolver BootstrapMethods I J K * L M N O P "haven/res/lib/itemtex/ItemTex$Icon java/lang/Object Q haven/GSprite$Owner Owner 7(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object; haven/OwnerContext haven/Resource
 R S &(Ljava/lang/Object;)Ljava/lang/Object;
  T apply ()Ljava/util/function/Function; add R(Ljava/lang/Class;Ljava/util/function/Function;)Lhaven/OwnerContext$ClassResolver; haven/res/lib/itemtex/ItemTex haven/GSprite U V Y ) * "java/lang/invoke/LambdaMetafactory metafactory [ Lookup �(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; \ %java/lang/invoke/MethodHandles$Lookup java/lang/invoke/MethodHandles itemtex.cjava !     
                              3     *� *+� *,� �              	       !          �                "  # $     !     	� +*� �                %    &          *� �             ' (           � Y� �           
 ) *          *� �             + ,     5      � Y� 	
�   � � �                 9     :  ; < = -    ]    *  
 7 	  5  	  @  	  C E	 W Z X code o  haven.res.lib.itemtex.ItemTex$1 ����   4 	  
     val$timg Ljava/awt/image/BufferedImage; <init> .(Lhaven/Coord;Ljava/awt/image/BufferedImage;)V Code LineNumberTable fill  ()Ljava/awt/image/BufferedImage; 
SourceFile ItemTex.java EnclosingMethod        haven/res/lib/itemtex/ItemTex$1 InnerClasses 
haven/TexL haven/res/lib/itemtex/ItemTex fixup ,(Ljava/awt/image/BufferedImage;)Lhaven/TexL; (Lhaven/Coord;)V itemtex.cjava 0                	   #     *,� *+� �    
       W     	        *� �    
       Y          
             code s  haven.res.lib.itemtex.ItemTex ����   4
 C f
 g h	 g i  � ��� j
 g k
 g l
  m n p q 
 r s t u v w
  x
  y
 z { |  ~	 z  � �	  �
  � �	  �
 g �	 B � � � �
  �
 B �
 B � � �
 � �	 - �	 - �
 � �@       
 � �
 � �
 � � �
 - �
 � �
  �
 � �
 � �	 B � � � �
 B � �
 7 �	 � �
 4 �	 4 �	 � �
 � �
 � �
 � � �
 @ f � � Icon InnerClasses made Ljava/util/Map; 	Signature ALjava/util/Map<Lhaven/MessageBuf;Ljava/awt/image/BufferedImage;>; fixed ;Ljava/util/Map<Ljava/awt/image/BufferedImage;Lhaven/TexL;>; <init> ()V Code LineNumberTable mkspr 4(Lhaven/OwnerContext;Lhaven/Message;)Lhaven/GSprite; StackMapTable � sprimg /(Lhaven/GSprite;)Ljava/awt/image/BufferedImage; create C(Lhaven/OwnerContext;Lhaven/Message;)Ljava/awt/image/BufferedImage; j � � � � fixsz >(Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage; fixup ,(Ljava/awt/image/BufferedImage;)Lhaven/TexL; � � <clinit> 
SourceFile ItemTex.java L M � � � � � haven/MessageBuf � � � � L � haven/Resource$Resolver Resolver � � � � � � � � haven/Resource "haven/res/lib/itemtex/ItemTex$Icon  haven/res/lib/mapres/ResourceMap L � L � � V � haven/GSprite$ImageSprite ImageSprite � � � � � � � � � � � haven/Resource$Image Image � � � � F G � � � java/awt/image/BufferedImage � � P Q T U � � � � � � � � � � � � � � � � � � haven/Coord L � � � � � � � � � � M J G 
haven/TexL � � � ] ^ haven/res/lib/itemtex/ItemTex$1 L � �  �	
 haven/CacheMap haven/res/lib/itemtex/ItemTex java/lang/Object haven/Message haven/OwnerContext java/lang/Throwable uint16 ()I nil Lhaven/Message; uint8 bytes (I)[B ([B)V context %(Ljava/lang/Class;)Ljava/lang/Object; getres (I)Lhaven/Indir; haven/Indir get ()Ljava/lang/Object; +(Lhaven/Resource$Resolver;Lhaven/Message;)V ,(Lhaven/Resource;Lhaven/Resource$Resolver;)V haven/GSprite Owner E(Lhaven/GSprite$Owner;Lhaven/Resource;Lhaven/Message;)Lhaven/GSprite; image  ()Ljava/awt/image/BufferedImage; owner Lhaven/GSprite$Owner; haven/GSprite$Owner ()Lhaven/Resource; imgc Ljava/lang/Class; layer Layer )(Ljava/lang/Class;)Lhaven/Resource$Layer; img Ljava/awt/image/BufferedImage; ()[B java/util/Map &(Ljava/lang/Object;)Ljava/lang/Object; clone ()Lhaven/MessageBuf; put 8(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; haven/PUtils imgsz -(Ljava/awt/image/BufferedImage;)Lhaven/Coord; x I y java/lang/Math max (II)I log (D)D round (D)J pow (DD)D (II)V 
haven/TexI mkbuf -(Lhaven/Coord;)Ljava/awt/image/BufferedImage; getGraphics ()Ljava/awt/Graphics; java/awt/Graphics 	drawImage 9(Ljava/awt/Image;IIIIIIIILjava/awt/image/ImageObserver;)Z dispose 	haven/Tex nextp2 (I)I .(Lhaven/Coord;Ljava/awt/image/BufferedImage;)V haven/Mipmapper dav Lhaven/Mipmapper; mipmap (Lhaven/Mipmapper;)V 	Sampler2D "Lhaven/render/Texture2D$Sampler2D; haven/render/Texture$Filter Filter LINEAR Lhaven/render/Texture$Filter;  haven/render/Texture2D$Sampler2D 	magfilter Sampler =(Lhaven/render/Texture$Filter;)Lhaven/render/Texture$Sampler; haven/render/Texture$Sampler 	minfilter 	mipfilter haven/Resource$Layer haven/render/Texture haven/render/Texture2D itemtex.cjava ! B C     F G  H    I  J G  H    K   L M  N        *� �    O        	 P Q  N   �     g+� =� N~� ~=� Y++� � � 	N*
�  � 
�  �  � :� Y� Y*
�  � 
+� � :-� �    R   	 � % S O   "    !  " 	 #  $  % % ' @ ( ^ ) 	 T U  N   P     '*� � *� �  �*� �  � � � � �    R     O       -  .  / 	 V W  N   �     I� Y+� � 	M� YN² ,�  � :� � ,*,�  � !� "Y:� # W-ð:-��   A B   B F B    R   $ � = X Y Z�   [ S X Y  \ O       4  5  6   7 % 8 = 9 B : 	 ] ^  N   �     �*� $L+� %+� &� '= (+� %�� * (� *o� +�� ,� +� (+� &�� * (� *o� +�� ,� +�� '>� -Y� .� /:� 0:+� %hl6+� &hl6*dldl`l`l+� %+� &� 1W� 2�    O   .    >  ?  @ 6 A K @ O B ] C d D x E � F � G 	 _ `  N  =     �� 3YM² 3*�  � 4L+� {*N-� $:� %� &� #� %� %� 5� � &� &� 5� -� 6N-� $:-:� 7Y� 8L+� 9� :+� ;� <� =� <� >� <� ?W� 3*+� # W,ç 
:,��+�   � �   � � �    R   : � L  Z a Y Z b  
� 7�   Z  Y  \�   Z a   O   >    M  N  O  P  Q  R L S Q T W V Z W f \ m ] � ^ � ` � a  c M  N   1      � @Y� A� � @Y� A� 3�    O   
    2 
 J  d    E   R 
  B D 	 7      
  o	  z }	   �  � z �	 �  � � 	 �@ �	codeentry    lib/mapres   