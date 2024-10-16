Haven Resource 1 src �  Environ.java /* Preprocessed source code */
/* $use: lib/globfx */

package haven.res.lib.env;

import haven.*;
import java.util.*;
import haven.res.lib.globfx.*;

public class Environ extends GlobData {
    private final Random rnd = new Random();
    
    float wdir = rnd.nextFloat() * (float)Math.PI * 2;
    float wvel = rnd.nextFloat() * 3;
    Coord3f gust = new Coord3f(0, 0, 0);
    Coord3f wind = Coord3f.o;
    private void wind(float dt) {
	Coord3f base = Coord3f.o.sadd(0, wdir, wvel);
	wdir += ((rnd.nextFloat() * 2) - 1) * 0.005;
	if(wdir < 0)
	    wdir += (float)Math.PI * 2;
	if(wdir > Math.PI * 2)
	    wdir -= (float)Math.PI * 2;
	wvel += rnd.nextFloat() * 0.005;
	if(wvel < 0)
	    wvel = 0;
	if(wvel > 20)
	    wvel = 20;
	if(rnd.nextInt(2000) == 0) {
	    gust.x = rnd.nextFloat() * 200 - 100;
	    gust.y = rnd.nextFloat() * 200 - 100;
	}
	float df = (float)Math.pow(0.2, dt);
	gust.x *= df;
	gust.y *= df;
	gust.z *= df;
	wind = base.add(gust);
    }
    
    public Coord3f wind() {
	return(wind);
    }

    public boolean tick(float dt) {
	wind(dt);
	return(false);
    }

    public static Environ get(Glob glob) {
	return(GlobEffector.getdata(glob, new Environ()));
    }
}
code ~  haven.res.lib.env.Environ ����   4 g
 % < =
  <	 " >
  ? @@I�	 " A@@  	 " B C
  D	 " E	  F	 " G
  H?tz�G�{@��@!�TD-A�  
  ICH  B�  	  J	  K?ə�����
  L	  M
  N
 " O P
 " <
 Q R S rnd Ljava/util/Random; wdir F wvel gust Lhaven/Coord3f; wind <init> ()V Code LineNumberTable (F)V StackMapTable C ()Lhaven/Coord3f; tick (F)Z get )(Lhaven/Glob;)Lhaven/res/lib/env/Environ; 
SourceFile Environ.java . / java/util/Random & ' T U java/lang/Math ( ) * ) haven/Coord3f . V + , W , - , X Y Z [ \ ) ] ) ^ _ ` ) a b - 2 haven/res/lib/env/Environ c d e haven/res/lib/globfx/GlobData 	nextFloat ()F (FFF)V o sadd (FFF)Lhaven/Coord3f; nextInt (I)I x y pow (DD)D z add  (Lhaven/Coord3f;)Lhaven/Coord3f; !haven/res/lib/globfx/GlobEffector getdata F(Lhaven/Glob;Lhaven/res/lib/globfx/Datum;)Lhaven/res/lib/globfx/Datum; 	env.cjava ! " %     & '     ( )     * )     + ,     - ,     . /  0   o     C*� *� Y� � **� � jj� **� � 	j� 
*� Y� � *� � �    1       	  
     -  ;   - 2  0  t     �� *� *� 
� M*Y� �*� � jf� kc�� *� �� *Y� b� *� � �� *Y� f� *Y� 
�*� � � kc�� 
*� 
�� *� 
*� 
�� 	*� 
*� ж � +*� *� � jf� *� *� � jf�  #�� �F*� Y� %j� *� Y� %j� *� Y� %j� *,*� �  � �    3    � ? 4$4 1   R       +  4  ?  K  V  m  v  {  �  �  �  �  �   � ! � " � # � $ � %  - 5  0        *� �    1       (  6 7  0   #     *#� !�    1   
    ,  - 	 8 9  0   '     *� "Y� #� $� "�    1       1  :    fcodeentry    lib/globfx   