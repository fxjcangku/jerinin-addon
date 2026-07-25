# ================================================================
#  CobbleSeller ProGuard 混淆规则
#  源码不受影响，仅编译后 jar 被混淆
# ================================================================

# ---- 保留 Fabric 入口 ----
-keep public class com.example.addon.AddonTemplate {
    public *;
}

# ---- 保留所有 Module 子类（Meteor 反射需要） ----
-keep public class * extends meteordevelopment.meteorclient.systems.modules.Module {
    public <init>(...);
    public *;
}

# ---- 保留 Meteor 事件处理（Orbit 反射） ----
-keepclassmembers class * {
    @meteordevelopment.orbit.EventHandler *;
}

# ---- 保留 Meteor Settings（反射创建） ----
-keep class meteordevelopment.meteorclient.settings.** { *; }

# ---- 保留 Minecraft 接口 ----
-keep class net.minecraft.** { *; }
-keep class com.mojang.** { *; }

# ---- 保留序列化 ----
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---- 混淆选项 ----
-repackageclasses ''
-allowaccessmodification
-dontshrink
-dontoptimize
-overloadaggressively
-useuniqueclassmembernames

# ---- 去调试信息 ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- 忽略缺失引用 ----
-dontwarn
-ignorewarnings
