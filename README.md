# HaowangButterKnife
##前言
在开发经常碰到注解形式,比如我们代码中常见@Override,也比如我之前发表的[Retrofit之解析xml (详细)](https://www.jianshu.com/p/4c9489fbc2aa)全是注解操作,还有很多第三方注解注入框架,像ButterKnife,Daggers等,注解的使用很广泛,今日以ButterKnife为例,来探其究竟.
###准备知识
java注解是在java5之后引入的,对于java注解完全不了解的请看[秒懂，Java 注解 （Annotation）你可以这样学](http://m.blog.csdn.net/briblue/article/details/73824058),看完可了解注解的基本用法.注解分为运行时注解和编译时注解,此处ButterKnife为编译时注解.
###核心原理
编译时注解是通过APT(Annotation Processing Tools)实现. APT是一种处理注释的工具,它对源代码文件进行检测找出其中的Annotation，使用Annotation进行额外的处理。 Annotation处理器在处理Annotation时可以根据源文件中的Annotation生成额外的源文件和其它的文件(文件具体内容由Annotation处理器的编写者决定),APT还会编译生成的源文件和原来的源文件，将它们一起生成class文件。
### 项目架构
![](http://upload-images.jianshu.io/upload_images/2236459-1c5dc456367e22c7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/840)在Inject中关联inject-annotion![](http://upload-images.jianshu.io/upload_images/2236459-b2e6b014eb491213.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)在Inject-compiler中引入jar包,如图![](http://upload-images.jianshu.io/upload_images/2236459-06f0f01c1797f8b5.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
### Android库Inject处理
Inject为使用方提供方法,先看是MainAcitity中是如何调用的
```
public class MainActivity extends AppCompatActivity {
    @BindView(R.id.text)
    TextView textview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InjectView.bind(this);
        Toast.makeText(this,"--->  "+textview, Toast.LENGTH_SHORT).show();
        textview.setText("6666666");
    }
}
```
所以此处在InjectView对应处理是,提供相应方法
```
public class InjectView {
    public  static  void bind(Activity activity)
    {
        String clsName=activity.getClass().getName();//反射拿到使用方类名
        try {
            Class<?> viewBidClass= Class.forName(clsName+"$$ViewBinder");//反射拿到新生成的内部类
            ViewBinder viewBinder= (ViewBinder) viewBidClass.newInstance();//反射后类型
            viewBinder.bind(activity);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }
}
```
ViewBinder是为了接收拿到的反射后的类型
```
public interface ViewBinder <T>{
    void  bind(T tartget);
}
```
###Java库inject-annotion处理
inject-annotion负责提供注解,此处只创建了BindView注解
```
@Target(ElementType.FIELD)//表示可以给属性进行注解
@Retention(RetentionPolicy.CLASS)//代表运行在编译时
public @interface BindView {
    int value();
}
```
###Java库Inject-compiler处理
Inject-compiler中处理核心逻辑,基本原理是,在编译时javac编译器会检查AbstractProcessor的子类，并且调用该类型的process函数，然后将添加了注解的所有元素都传递到process函数中，使得开发人员可以在编译器进行相应的处理.
此处BindViewProcessor继承了AbstractProcessor,并通过@AutoService(Processor.class),表明自身为注解处理器,其中主要有四个方法,在init方法中做初始化;在getSupportedAnnotationTypes方法中指明支持哪些注解,此处指明的注解即为BindView;在getSupportedSourceVersion方法中返回最新的JDK版本;在process方法中做主逻辑处理
```
//用@AutoService 指定注解处理器
@AutoService(Processor.class)
public class BindViewProcessor extends AbstractProcessor {
    /**
     * 处理Element工具类
     * 主要获取包名
     */
    private Elements elementUtils;
    /**
     * Java文件输出类（非常重要）生成它是用来java文件
     */
    private Filer    filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
        filer = processingEnvironment.getFiler();
    }

    /**
     * @return 当前注解处理器  支持哪些注解
     * 返回的是set字符串集合
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(BindView.class.getCanonicalName());
        //        types.add(Override.class.getCanonicalName());
        return types;
    }

    /**
     * 支持jdk版本
     * 一般选择最新版本
     *
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * javac编译器  遇到含有BindView注解的java文件时，就会调用这个process方法
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //声明一个缓存的map集合   键  是Activity   值  是当前Activity 里面含有BndView成员变量的集合
        Map<TypeElement, List<FieldViewBinding>> targetMap = new HashMap<>();
        //编译时只能通过文件输出打印，不能通过Log，System.out.print打印
        FileUtils.print("------------>    ");
        //for循环带有BindView注解的Element
        //将每个Element进行分组。分组的形式 是将在一个Activit的Element分为一组
        for (Element element : roundEnvironment.getElementsAnnotatedWith(BindView.class)) {
            FileUtils.print("elment   " + element.getSimpleName().toString());
            //  enClosingElement可以理解成Element
            TypeElement enClosingElement = (TypeElement) element.getEnclosingElement();
            // List<FieldViewBinding>   当前Activity 含有注解的成员变量集合
            List<FieldViewBinding> list = targetMap.get(enClosingElement);
            if (list == null) {
                list = new ArrayList<>();
                targetMap.put(enClosingElement, list);//
            }
            //得到包名
            String packageName = getPackageName(enClosingElement);
            //得到id
            int id = element.getAnnotation(BindView.class).value();
            //            得到成员变量名  TextView  text;  这里得到的是text字符串
            String fieldName = element.getSimpleName().toString();
            //            当前成员变量的类类型   可以理解成  TextView
            TypeMirror typeMirror = element.asType();
            //            封装成FieldViewBinding  类型
            FieldViewBinding fieldViewBinding = new FieldViewBinding(fieldName, typeMirror, id);
            list.add(fieldViewBinding);
        }
        //遍历每一个Activity  TypeElement代表类类型
        for (Map.Entry<TypeElement, List<FieldViewBinding>> item : targetMap.entrySet()) {
            List<FieldViewBinding> list = item.getValue();

            if (list == null || list.size() == 0) {
                continue;
            }
            //enClosingElement 表示 activity
            TypeElement enClosingElement = item.getKey();
            //            得到包名
            String packageName = getPackageName(enClosingElement);
            //截取字符串    MainActivity
            String complite = getClassName(enClosingElement, packageName);
            //遵循Javapoet规范，MainActivity为类类型  在这里封装成ClassName
            ClassName className = ClassName.bestGuess(complite);
            //          ViewBinder类型
            ClassName viewBinder = ClassName.get("com.base.inject", "ViewBinder");
            //            开始构建java文件
            //            从外层包名  类名开始构建
            TypeSpec.Builder result = TypeSpec.classBuilder(complite + "$$ViewBinder")
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariable(TypeVariableName.get("T", className))
                    .addSuperinterface(ParameterizedTypeName.get(viewBinder, className));
            //          构建方法名
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("bind")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.VOID)
                    .addAnnotation(Override.class)
                    .addParameter(className, "target", Modifier.FINAL);
            //            构建方法里面的具体逻辑，这里的逻辑代表findViewById
            for (int i = 0; i < list.size(); i++) {
                FieldViewBinding fieldViewBinding = list.get(i);
                //-->android.text.TextView
                String pacckageNameString = fieldViewBinding.getType().toString();
                ClassName viewClass = ClassName.bestGuess(pacckageNameString);
                //$L  代表占位符  和StringFormater类似。$L代表基本类型  $T代表  类类型
                methodBuilder.addStatement
                        ("target.$L=($T)target.findViewById($L)", fieldViewBinding.getName()
                                , viewClass, fieldViewBinding.getResId());
            }

            result.addMethod(methodBuilder.build());

            try {
                //生成Java文件头部的注释说明，装逼用
                JavaFile.builder(packageName, result.build())
                        .addFileComment("auto create make")
                        .build().writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        return false;
    }

    //enClosingElement.getQualifiedName().toString()返回的是com.example.administrator.butterdepends.MainActivity
    private String getClassName(TypeElement enClosingElement, String packageName) {
        int packageLength = packageName.length() + 1;
        //        replace(".","$")的意思是  如果当前的enClosing为内部类的话
        //       裁剪掉包名和最后一个点号，将去掉包名后，后面还有点号则替换成$符号
        return enClosingElement.getQualifiedName().toString().substring(packageLength).replace(".", "$");
    }

    private String getPackageName(TypeElement enClosingElement) {
        //返回的是  com.example.administrator.butterknifeframwork。通过工具类获取的
        return elementUtils.getPackageOf(enClosingElement).getQualifiedName().toString();
    }
}
```
此处重点说明process方法,第一个for循环用来拿到所有使用方class,以及该class调用的所有注解,所以建立一个map集合,TypeElement对应的是使用方class文件,List<FieldViewBinding>>代表该classs文件中用到的所有注解.最早返回的是Element形式,此处将其转化为FieldViewBinding来存储.
FieldViewBinding代码
 ```
public class FieldViewBinding {
    private String name;//  textview
    private TypeMirror type ;//--->TextView
    private int resId;//--->R.id.textiew

    public FieldViewBinding(String name, TypeMirror type, int resId) {
        this.name = name;
        this.type = type;
        this.resId = resId;
    }

    public String getName() {
        return name;
    }

    public TypeMirror getType() {
        return type;
    }

    public int getResId() {
        return resId;
    }
}
```
第二个for循环是生成每个调用方class相对应的内部类class文件,是由外往内拼接生成的,先拼接创建包名类名,然后拼接构建方法名,最终的拼接效果为![](http://upload-images.jianshu.io/upload_images/2236459-ba1c4fd9c273cd38.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)代码运行成功可以看到这个新生成的class文件,其实就是一个对应的内部类
注意:在jar包中测试代码不能使用log日志,所以此处使用FileUtils,以文件输出打印,会在桌面生成一个log.txt文件显示所有log信息,以便于代码出bug时进行调试,奉上FileUtils代码
```
public class FileUtils {
    public static void print(String text)
    {
        File file=new File("C:\\Users\\Administrator\\Desktop\\log.txt");
        if(!file.exists())
        {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter fileWriter=new FileWriter(file.getAbsoluteFile(),true);
            fileWriter.write(text+"\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```
以上为手写注入ButterKnife的全部流程,其实并不复杂,因为只是简单实现效果,Dagger2等也是相同的道理
此框架和ButterKnife使用方法是一致的,使用时别忘了在gradle中加入![](http://upload-images.jianshu.io/upload_images/2236459-c391eb79c2bb49a1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

##结尾
奉上代码地址[https://github.com/HaowangSmith/HaowangButterKnife](https://github.com/HaowangSmith/HaowangButterKnife)
相关知识链接[ButterKnife源码分析](https://www.jianshu.com/p/0f3f4f7ca505)








