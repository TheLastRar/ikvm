
namespace IKVM.Java.Externs.sun.misc
{
    internal interface IUnsafeFieldAccessor
    {
        public T GetField<T>(object o, long offset);
        public void PutField<T>(object o, long offset, T value);

        public object staticFieldBase(object self, global::java.lang.reflect.Field f);
        public long staticFieldOffset(object self, global::java.lang.reflect.Field f);

        public long objectFieldOffset(object self, global::java.lang.reflect.Field f);

        public T GetFieldVolatile<T>(object o, long offset);
        public void PutFieldVolatile<T>(object o, long offset, T value);

        bool CompareAndSwapField<T>(object o, long offset, T expected, T value);
    }
}
