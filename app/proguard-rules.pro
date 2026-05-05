-keepattributes *Annotation*
-keep class com.codeai.editor.data.model.** { *; }
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
