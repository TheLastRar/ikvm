using IKVM.Internal;
using IKVM.Java.Externs.java.io;
using IKVM.Runtime;
using System;
using System.Reflection;
using System.Reflection.Emit;
using System.Runtime.InteropServices;
using System.Runtime.Serialization;
using System.Threading;

namespace IKVM.Java.Externs.sun.misc
{
    public interface IUnsafeDirectImpl
    {
        public T GetField<T>(object o, long offset);
        public void PutField<T>(object o, long offset, T value);

        public T GetFieldVolatile<T>(object o, long offset);
        public void PutFieldVolatile<T>(object o, long offset, T value);

        bool CompareAndSwapField<T>(object o, long offset, T expected, T value);
    }
    internal class UnsafeDirect : IUnsafeFieldAccessor
    {
        class HeaderSizer { public int mark; }

        private readonly int HeaderSize = 0;
        private static readonly MethodInfo makeObj = typeof(FormatterServices).GetMethod("GetUninitializedObject");
        private static readonly MethodInfo getTypeFromHandle = typeof(Type).GetMethod("GetTypeFromHandle");

        IUnsafeDirectImpl objectAccessor;
        UnsafeTypeWrapper staticAccessor = new UnsafeTypeWrapper();

        public UnsafeDirect()
        {
            var dm = DynamicMethodUtil.Create($"__<UnsafeGetCLRHeaderSize>", typeof(UnsafeDirect), true, typeof(int), Type.EmptyTypes);

            var il = dm.GetILGenerator();
            // Make object
            il.Emit(OpCodes.Newobj, typeof(HeaderSizer).GetConstructor(Type.EmptyTypes));
            // Pin object
            il.DeclareLocal(typeof(object), true);
            il.Emit(OpCodes.Stloc_0);
            // Get field Address
            il.Emit(OpCodes.Ldloc_0);
            il.Emit(OpCodes.Ldflda, typeof(HeaderSizer).GetField("mark"));
            il.Emit(OpCodes.Conv_I);
            // subtract from 
            il.Emit(OpCodes.Ldloc_0);
            il.Emit(OpCodes.Conv_I);
            il.Emit(OpCodes.Sub);
            // Convert to int
            il.Emit(OpCodes.Conv_I4);
            il.Emit(OpCodes.Ret);
            HeaderSize = ((Func<int>)dm.CreateDelegate(typeof(Func<int>))).Invoke();

            var assemblyName = new AssemblyName("UnsafeHelper");
            var helperAssembly = AssemblyBuilder.DefineDynamicAssembly(assemblyName, AssemblyBuilderAccess.Run);
            var helperModule = helperAssembly.DefineDynamicModule(assemblyName.Name);

            var iltype = helperModule.DefineType("UnsafeILImpl");
            iltype.AddInterfaceImplementation(typeof(IUnsafeDirectImpl));

            BuildFieldAccessorMethod(iltype.DefineMethod("GetField", MethodAttributes.Public | MethodAttributes.Final | MethodAttributes.Virtual), false);
            BuildFieldAccessorMethod(iltype.DefineMethod("PutField", MethodAttributes.Public | MethodAttributes.Final | MethodAttributes.Virtual), true);
            BuildVolatileFieldAccessorMethod(iltype.DefineMethod("GetFieldVolatile", MethodAttributes.Public | MethodAttributes.Final | MethodAttributes.Virtual), false);
            BuildVolatileFieldAccessorMethod(iltype.DefineMethod("PutFieldVolatile", MethodAttributes.Public | MethodAttributes.Final | MethodAttributes.Virtual), true);

            var csb = iltype.DefineMethod("CompareAndSwapField", MethodAttributes.Public | MethodAttributes.Final | MethodAttributes.Virtual);
            var genericType = csb.DefineGenericParameters(new string[] { "T" })[0];
            csb.SetParameters(typeof(object), typeof(long), genericType, genericType);
            csb.SetReturnType(typeof(bool));
            
            #region "CompareAndSwapField"
            il = csb.GetILGenerator();
            // Pin object
            il.DeclareLocal(typeof(object), true);
            il.Emit(OpCodes.Ldarg_1);
            il.Emit(OpCodes.Stloc_0);
            // Calc address
            il.Emit(OpCodes.Ldloc_0);
            il.Emit(OpCodes.Conv_I);
            il.Emit(OpCodes.Ldarg_1);
            il.Emit(OpCodes.Conv_I);
            il.Emit(OpCodes.Add);
            il.Emit(OpCodes.Ldc_I4, HeaderSize);
            il.Emit(OpCodes.Add);

            // Push rest of arguments
            il.Emit(OpCodes.Ldarg_2);
            il.Emit(OpCodes.Ldarg_3);

            // Determine which CompareAndSwap to call
            var obj = il.DefineLabel();
            var byt = il.DefineLabel();
            var i16 = il.DefineLabel();
            var i32 = il.DefineLabel();
            var i64 = il.DefineLabel();
            var flt = il.DefineLabel();
            var dbl = il.DefineLabel();

            //MethodInfo getHandleFromType = typeof(Type).GetMethod("GetTypeHandle");
            MethodInfo typeEqual = typeof(Type).GetMethod("Equals", new[] { typeof(Type) });

            il.Emit(OpCodes.Ldtoken, genericType);
            il.Emit(OpCodes.Call, getTypeFromHandle);

            il.Emit(OpCodes.Dup);
            il.Emit(OpCodes.Call, typeof(Type).GetProperty("IsPrimitive").GetGetMethod());
            il.Emit(OpCodes.Brfalse, obj);

            il.Emit(OpCodes.Dup);
            il.Emit(OpCodes.Ldtoken, typeof(byte));
            il.Emit(OpCodes.Call, getTypeFromHandle);
            il.Emit(OpCodes.Call, typeEqual);
            il.Emit(OpCodes.Brtrue, byt);

            il.Emit(OpCodes.Dup);
            il.Emit(OpCodes.Ldtoken, typeof(bool));
            il.Emit(OpCodes.Call, getTypeFromHandle);
            il.Emit(OpCodes.Call, typeEqual);
            il.Emit(OpCodes.Brtrue, byt);

            il.Emit(OpCodes.Dup);
            il.Emit(OpCodes.Ldtoken, typeof(short));
            il.Emit(OpCodes.Call, getTypeFromHandle);
            il.Emit(OpCodes.Call, typeEqual);
            il.Emit(OpCodes.Brtrue, i16);

            il.Emit(OpCodes.Dup);
            il.Emit(OpCodes.Ldtoken, typeof(char));
            il.Emit(OpCodes.Call, getTypeFromHandle);
            il.Emit(OpCodes.Call, typeEqual);
            il.Emit(OpCodes.Brtrue, i16);

            il.Emit(OpCodes.Dup);
            il.Emit(OpCodes.Ldtoken, typeof(int));
            il.Emit(OpCodes.Call, getTypeFromHandle);
            il.Emit(OpCodes.Call, typeEqual);
            il.Emit(OpCodes.Brtrue, i32);

            il.Emit(OpCodes.Dup);
            il.Emit(OpCodes.Ldtoken, typeof(long));
            il.Emit(OpCodes.Call, getTypeFromHandle);
            il.Emit(OpCodes.Call, typeEqual);
            il.Emit(OpCodes.Brtrue, i64);

            il.Emit(OpCodes.Dup);
            il.Emit(OpCodes.Ldtoken, typeof(double));
            il.Emit(OpCodes.Call, getTypeFromHandle);
            il.Emit(OpCodes.Call, typeEqual);
            il.Emit(OpCodes.Brtrue, dbl);

            // Throw Exception
            // Fallthough from above
            il.MarkLabel(byt);
            il.MarkLabel(i16);
            il.MarkLabel(flt);
#if FIRST_PASS
            il.Emit(OpCodes.Newobj, typeof(NotImplementedException).GetConstructor(Type.EmptyTypes));
#else
            il.Emit(OpCodes.Newobj, typeof(global::java.lang.InternalError).GetConstructor(Type.EmptyTypes));
#endif
            il.Emit(OpCodes.Throw);

            // Call CompareAndSwap

            il.MarkLabel(obj);
            il.Emit(OpCodes.Pop);
            il.Emit(OpCodes.Call, ByteCodeHelperMethods.CompareAndSwapObject);
            il.Emit(OpCodes.Ret);

            il.MarkLabel(i32);
            il.Emit(OpCodes.Pop);
            il.Emit(OpCodes.Call, ByteCodeHelperMethods.CompareAndSwapInt);
            il.Emit(OpCodes.Ret);

            il.MarkLabel(i64);
            il.Emit(OpCodes.Pop);
            il.Emit(OpCodes.Call, ByteCodeHelperMethods.CompareAndSwapLong);
            il.Emit(OpCodes.Ret);

            il.MarkLabel(dbl);
            il.Emit(OpCodes.Pop);
            il.Emit(OpCodes.Call, ByteCodeHelperMethods.CompareAndSwapDouble);
            il.Emit(OpCodes.Ret);
            #endregion
            Type ilt;
            try
            {
                ilt = iltype.CreateType();
                objectAccessor = (IUnsafeDirectImpl)Activator.CreateInstance(ilt);
            }
            catch (Exception e)
            {
                System.Console.WriteLine(e.Message);
                throw;
            }
            System.Console.WriteLine("Done Static Constructor");
        }

        void BuildFieldAccessorMethod(MethodBuilder mb, bool write)
        {
            var genericType = mb.DefineGenericParameters(new string[] { "T" })[0];
            if (write)
            {
                mb.SetParameters(typeof(object), typeof(long), genericType);
                mb.SetReturnType(typeof(void));
            }
            else
            {
                mb.SetParameters(typeof(object), typeof(long));
                mb.SetReturnType(genericType);
            }
            var il = mb.GetILGenerator();
            if (!write)
            {
                il.Emit(OpCodes.Ldc_I4_0);
            }
            il.Emit(OpCodes.Ret);
            // Pin object
            il.DeclareLocal(typeof(object), true);
            il.Emit(OpCodes.Ldarg_1);
            il.Emit(OpCodes.Stloc_0);
            // Calc address
            il.Emit(OpCodes.Ldloc_0);
            il.Emit(OpCodes.Conv_I);
            il.Emit(OpCodes.Ldarg_1);
            il.Emit(OpCodes.Conv_I);
            il.Emit(OpCodes.Add);
            il.Emit(OpCodes.Ldc_I4, HeaderSize);
            il.Emit(OpCodes.Add);
            // Perform operation
            if (write)
            {
                il.Emit(OpCodes.Ldarg_2);
                il.Emit(OpCodes.Stobj, genericType);
            }
            else
                il.Emit(OpCodes.Ldobj, genericType);
            // return
            il.Emit(OpCodes.Ret);
        }

        // In Java, a volatile operation is atomic.
        // For 64bit types, we need to use Interlock
        // For object references, we can use the volatile op
        void BuildVolatileFieldAccessorMethod(MethodBuilder mb, bool write)
        {
            var genericType = mb.DefineGenericParameters(new string[] { "T" })[0];
            if (write)
            {
                mb.SetParameters(typeof(object), typeof(long), genericType);
                mb.SetReturnType(typeof(void));
            }
            else
            {
                mb.SetParameters(typeof(object), typeof(long));
                mb.SetReturnType(genericType);
            }
            var il = mb.GetILGenerator();
            if (!write)
            {
                il.Emit(OpCodes.Ldc_I4_0);
            }
            il.Emit(OpCodes.Ret);
            // Pin object
            il.DeclareLocal(typeof(object), true);
            il.Emit(OpCodes.Ldarg_1);
            il.Emit(OpCodes.Stloc_0);
            // Calc address
            il.Emit(OpCodes.Ldloc_0);
            il.Emit(OpCodes.Conv_I);
            il.Emit(OpCodes.Ldarg_1);
            il.Emit(OpCodes.Conv_I);
            il.Emit(OpCodes.Add);
            il.Emit(OpCodes.Ldc_I4, HeaderSize);
            il.Emit(OpCodes.Add);

            // if writing, load value here
            if (write)
                il.Emit(OpCodes.Ldarg_2);

            // Determine kindof operation can we do
            var direct = il.DefineLabel();
            var interlock = il.DefineLabel();

            // Check Size
            il.Emit(OpCodes.Sizeof, genericType);
            il.Emit(OpCodes.Ldc_I4_4);
            il.Emit(OpCodes.Ble_Un_S, direct);
            // Check if primative type
            il.Emit(OpCodes.Ldtoken, genericType);
            il.Emit(OpCodes.Call, typeof(Type).GetMethod("GetTypeFromHandle"));
            il.Emit(OpCodes.Dup); // Dupe for interlock type check
            il.Emit(OpCodes.Call, typeof(Type).GetProperty("IsPrimitive").GetGetMethod());
            il.Emit(OpCodes.Brfalse_S, direct);

            // Perform Interlocked operation
            il.MarkLabel(interlock);
            var lg = il.DefineLabel();
            var dbl = il.DefineLabel();
            // Only long and double
            il.Emit(OpCodes.Ldtoken, typeof(Double));
            il.Emit(OpCodes.Call, typeof(Type).GetMethod("GetTypeFromHandle"));
            il.Emit(OpCodes.Beq_S, dbl);

            il.MarkLabel(lg);
            il.Emit(OpCodes.Call, write ? ByteCodeHelperMethods.VolatileWriteLong : ByteCodeHelperMethods.VolatileReadLong);
            il.Emit(OpCodes.Ret);

            il.MarkLabel(dbl);
            il.Emit(OpCodes.Call, write ? ByteCodeHelperMethods.VolatileWriteDouble : ByteCodeHelperMethods.VolatileReadDouble);
            il.Emit(OpCodes.Ret);

            // Perform Direct operation
            il.MarkLabel(direct);
            il.Emit(OpCodes.Pop);

            il.Emit(OpCodes.Volatile);
            il.Emit(write ? OpCodes.Stobj : OpCodes.Ldobj, genericType);
            il.Emit(OpCodes.Call, typeof(Thread).GetMethod("MemoryBarrier"));
            il.Emit(OpCodes.Ret);
        }

        public T GetField<T>(object o, long offset)
        {
            //if (o is TypeWrapper w)
                return staticAccessor.GetField<T>(o, offset);
            //else
            //    return objectAccessor.GetField<T>(o, offset);
        }

        public void PutField<T>(object o, long offset, T value)
        {
            //if (o is TypeWrapper w)
                staticAccessor.PutField(o, offset, value);
           //else
           //     objectAccessor.PutField(o, offset, value);
        }

        public object staticFieldBase(object self, global::java.lang.reflect.Field f)
        {
            return staticAccessor.staticFieldBase(self, f);
        }

        public long staticFieldOffset(object self, global::java.lang.reflect.Field f)
        {
            return staticAccessor.staticFieldOffset(self, f);
        }

        public long objectFieldOffset(object self, global::java.lang.reflect.Field f)
        {
            var w = FieldWrapper.FromField(f);
            if (w.IsStatic)
                throw new global::java.lang.IllegalArgumentException();

            try
            {
                var fi = w.GetField();
                IntPtr a = Marshal.AllocHGlobal(32);
                var method = DynamicMethodUtil.Create($"__<GetFieldOffset>__{w.DeclaringType.Name.Replace(".", "_")}__{w.Name}", w.DeclaringType.TypeAsTBD, true, typeof(long), Type.EmptyTypes);
                System.Console.WriteLine($"Building {method.Name}()");
                var il = method.GetILGenerator();
                //Fake object (As we can't make abstract classes with GetUninitializedObject)
                //TODO, do allocation in IL
                il.Emit(OpCodes.Ldc_I8, a.ToInt64() + 16);
                il.Emit(OpCodes.Conv_I);
                //Pin object
                il.DeclareLocal(fi.DeclaringType, false);
                il.Emit(OpCodes.Stloc_0);
                //Get address
                il.Emit(OpCodes.Ldloc_0);
                il.Emit(OpCodes.Ldflda, fi);
                il.Emit(OpCodes.Conv_I);
                il.Emit(OpCodes.Ldloc_0);
                il.Emit(OpCodes.Conv_I);
                il.Emit(OpCodes.Sub);
                //Subtract HeaderSize
                il.Emit(OpCodes.Ldc_I4, HeaderSize);
                il.Emit(OpCodes.Sub);
                //Convert to long
                il.Emit(OpCodes.Conv_I8);
                il.Emit(OpCodes.Ret);

                Func<long> offsetHelper = (Func<long>)method.CreateDelegate(typeof(Func<long>));
                long offset = offsetHelper();
                System.Console.WriteLine($"{method.Name}() returned {offset}");
                Marshal.FreeHGlobal(a);
            }
            catch (Exception e)
            {
                System.Console.WriteLine(e.Message);
                throw;
            }
            //return offsetHelper();
            return staticAccessor.objectFieldOffset(self, f);
        }

        public T GetFieldVolatile<T>(object o, long offset)
        {
            //if (o is TypeWrapper w)
                return staticAccessor.GetFieldVolatile<T>(o, offset);
            //else
            //    return objectAccessor.GetFieldVolatile<T>(o, offset);
        }

        public void PutFieldVolatile<T>(object o, long offset, T value)
        {
            //if (o is TypeWrapper w)
                staticAccessor.PutFieldVolatile(o, offset, value);
            //else
            //    objectAccessor.PutFieldVolatile(o, offset, value);
        }

        public bool CompareAndSwapField<T>(object o, long offset, T expected, T value)
        {
            //if (o is TypeWrapper w)
                return staticAccessor.CompareAndSwapField(o, offset, expected, value);
            //else
            //    return objectAccessor.CompareAndSwapField(o, offset, expected, value);
        }
    }
}
