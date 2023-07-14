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
        private static readonly ConstructorInfo objectCtor = typeof(object).GetConstructor(Type.EmptyTypes);
        private static readonly MethodInfo getTypeFromHandle = typeof(Type).GetMethod("GetTypeFromHandle");
        private static readonly MethodInfo getIsPrimitive = typeof(Type).GetProperty("IsPrimitive").GetGetMethod();
        private static readonly MethodInfo typeEqual = typeof(Type).GetMethod("Equals", new[] { typeof(Type) });

        private readonly IUnsafeDirectImpl objectAccessor;
        private readonly UnsafeTypeWrapper staticAccessor = new UnsafeTypeWrapper();

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
            il.Emit(OpCodes.Conv_U);
            // subtract from 
            il.Emit(OpCodes.Ldloc_0);
            il.Emit(OpCodes.Conv_U);
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

            BuildCompareAndSwapMethod(iltype.DefineMethod("CompareAndSwapField", MethodAttributes.Public | MethodAttributes.Final | MethodAttributes.Virtual));

            Type ilt = iltype.CreateType();
            objectAccessor = (IUnsafeDirectImpl)Activator.CreateInstance(ilt);
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

            EmitPinAndCalcAddress(il);

            // Perform operation
            if (write)
            {
                il.Emit(OpCodes.Ldarg_3);
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

            EmitPinAndCalcAddress(il);

            // Determine kindof operation can we do
            var direct = il.DefineLabel();
            var interlock = il.DefineLabel();

            // Check Size
            il.Emit(OpCodes.Sizeof, genericType);
            il.Emit(OpCodes.Ldc_I4_4);
            il.Emit(OpCodes.Ble_Un_S, direct);
            // Check if primative type
            il.Emit(OpCodes.Ldtoken, genericType);
            il.Emit(OpCodes.Call, getTypeFromHandle);
            il.Emit(OpCodes.Dup); // Dupe for interlock type check
            il.Emit(OpCodes.Call, getIsPrimitive);
            il.Emit(OpCodes.Brtrue_S, interlock);
            il.Emit(OpCodes.Pop);

            // Perform Direct operation
            il.MarkLabel(direct);
            if (write)
            {
                il.Emit(OpCodes.Ldarg_3);
                il.Emit(OpCodes.Volatile);
                il.Emit(OpCodes.Stobj, genericType);
            }
            else
            {
                il.Emit(OpCodes.Volatile);
                il.Emit(OpCodes.Ldobj, genericType);
            }
            il.Emit(OpCodes.Call, typeof(Thread).GetMethod("MemoryBarrier"));
            // return
            il.Emit(OpCodes.Ret);

            // Perform Interlocked operation
            il.MarkLabel(interlock);
            il.BeginScope();
            if (!write)
                // Used for casting
                il.DeclareLocal(genericType);

            var lg = il.DefineLabel();
            var dbl = il.DefineLabel();
            // Only long and double
            il.Emit(OpCodes.Ldtoken, typeof(double));
            il.Emit(OpCodes.Call, getTypeFromHandle);
            il.Emit(OpCodes.Call, typeEqual);
            il.Emit(OpCodes.Brtrue_S, dbl);

            il.MarkLabel(lg);
            if (write)
            {
                il.Emit(OpCodes.Ldarga_S, 3);
                il.Emit(OpCodes.Ldind_I8);
                il.Emit(OpCodes.Call, ByteCodeHelperMethods.VolatileWriteLong);
            }
            else
            {
                il.Emit(OpCodes.Call, ByteCodeHelperMethods.VolatileReadLong);
                il.Emit(OpCodes.Stloc_1);
                il.Emit(OpCodes.Ldloc_1);
            }
            // return
            il.Emit(OpCodes.Ret);

            il.MarkLabel(dbl);
            if (write)
            {
                il.Emit(OpCodes.Ldarga_S, 3);
                il.Emit(OpCodes.Ldind_R8);
                il.Emit(OpCodes.Call, ByteCodeHelperMethods.VolatileWriteDouble);
            }
            else
            {
                il.Emit(OpCodes.Call, ByteCodeHelperMethods.VolatileReadDouble);
                il.Emit(OpCodes.Stloc_1);
                il.Emit(OpCodes.Ldloc_1);
            }
            // return
            il.Emit(OpCodes.Ret);
            il.EndScope();
        }

        void BuildCompareAndSwapMethod(MethodBuilder mb)
        {
            var genericType = mb.DefineGenericParameters(new string[] { "T" })[0];
            mb.SetParameters(typeof(object), typeof(long), genericType, genericType);
            mb.SetReturnType(typeof(bool));
            var il = mb.GetILGenerator();

            EmitPinAndCalcAddress(il);

            // Determine which CompareAndSwap to call
            // Only need to support obj, int & long
            var obj = il.DefineLabel();
            var i32 = il.DefineLabel();
            var i64 = il.DefineLabel();

            il.Emit(OpCodes.Ldtoken, genericType);
            il.Emit(OpCodes.Call, getTypeFromHandle);

            il.Emit(OpCodes.Dup);
            il.Emit(OpCodes.Call, getIsPrimitive);
            il.Emit(OpCodes.Brfalse_S, obj);

            il.Emit(OpCodes.Dup);
            il.Emit(OpCodes.Ldtoken, typeof(int));
            il.Emit(OpCodes.Call, getTypeFromHandle);
            il.Emit(OpCodes.Call, typeEqual);
            il.Emit(OpCodes.Brtrue_S, i32);

            il.Emit(OpCodes.Dup);
            il.Emit(OpCodes.Ldtoken, typeof(long));
            il.Emit(OpCodes.Call, getTypeFromHandle);
            il.Emit(OpCodes.Call, typeEqual);
            il.Emit(OpCodes.Brtrue_S, i64);

            // Throw Exception
            // Fallthough from above
#if FIRST_PASS
            il.Emit(OpCodes.Newobj, typeof(NotImplementedException).GetConstructor(Type.EmptyTypes));
#else
            il.Emit(OpCodes.Newobj, typeof(global::java.lang.InternalError).GetConstructor(Type.EmptyTypes));
#endif
            il.Emit(OpCodes.Throw);

            // Call CompareAndSwap

            il.MarkLabel(obj);
            il.Emit(OpCodes.Pop);
            // Push rest of arguments, with cast
            il.Emit(OpCodes.Ldarga_S, 3);
            il.Emit(OpCodes.Ldind_Ref);
            il.Emit(OpCodes.Ldarga_S, 4);
            il.Emit(OpCodes.Ldind_Ref);
            il.Emit(OpCodes.Call, ByteCodeHelperMethods.CompareAndSwapObject);
            il.Emit(OpCodes.Ret);

            il.MarkLabel(i32);
            il.Emit(OpCodes.Pop);
            // Push rest of arguments, with cast
            il.Emit(OpCodes.Ldarga_S, 3);
            il.Emit(OpCodes.Ldind_I4);
            il.Emit(OpCodes.Ldarga_S, 4);
            il.Emit(OpCodes.Ldind_I4);
            il.Emit(OpCodes.Call, ByteCodeHelperMethods.CompareAndSwapInt);
            il.Emit(OpCodes.Ret);

            il.MarkLabel(i64);
            il.Emit(OpCodes.Pop);
            // Push rest of arguments, with cast
            il.Emit(OpCodes.Ldarga_S, 3);
            il.Emit(OpCodes.Ldind_I8);
            il.Emit(OpCodes.Ldarga_S, 4);
            il.Emit(OpCodes.Ldind_I8);
            il.Emit(OpCodes.Call, ByteCodeHelperMethods.CompareAndSwapLong);
            il.Emit(OpCodes.Ret);
        }

        // Loads arg_1 (object obj) & arg_2 (long offset)
        // pins obj
        // Pushes combined ptr onto stack
        void EmitPinAndCalcAddress(ILGenerator il)
        {
            il.Emit(OpCodes.Ldarg_1);

            // Pin object
            il.DeclareLocal(typeof(object), true);
            il.Emit(OpCodes.Stloc_0);

            // Load & cast inputs
            il.Emit(OpCodes.Ldloc_0);
            il.Emit(OpCodes.Conv_U);
            il.Emit(OpCodes.Ldarg_2);
            il.Emit(OpCodes.Conv_U);
            il.Emit(OpCodes.Ldc_I4, HeaderSize);

            // Sum address & offsets
            il.Emit(OpCodes.Add);
            il.Emit(OpCodes.Add);
        }

        public T GetField<T>(object o, long offset)
        {
            if (o is TypeWrapper w)
                return staticAccessor.GetField<T>(o, offset);
            else
                return objectAccessor.GetField<T>(o, offset);
        }

        public void PutField<T>(object o, long offset, T value)
        {
            if (o is TypeWrapper w)
                staticAccessor.PutField(o, offset, value);
            else
                objectAccessor.PutField(o, offset, value);
        }

        public object StaticFieldBase(global::java.lang.reflect.Field f)
        {
            return staticAccessor.StaticFieldBase(f);
        }

        public long StaticFieldOffset(global::java.lang.reflect.Field f)
        {
            return staticAccessor.StaticFieldOffset(f);
        }

        public long ObjectFieldOffset(global::java.lang.reflect.Field f)
        {
            var w = FieldWrapper.FromField(f);
            if (w.IsStatic)
                throw new global::java.lang.IllegalArgumentException();

            System.Diagnostics.Debug.Assert(!w.DeclaringType.TypeAsTBD.IsValueType);

            w.ResolveField();
            var fi = w.GetField();
            var method = DynamicMethodUtil.Create($"__<GetFieldOffset>__{w.DeclaringType.Name.Replace(".", "_")}__{w.Name}", w.DeclaringType.TypeAsTBD, true, typeof(long), Type.EmptyTypes);
            var il = method.GetILGenerator();
            // New object
            il.Emit(OpCodes.Newobj, objectCtor);
            // Cast object
            il.DeclareLocal(w.DeclaringType.TypeAsTBD, true);
            il.Emit(OpCodes.Stloc_0);
            // Get address
            il.Emit(OpCodes.Ldloc_0);
            il.Emit(OpCodes.Ldflda, fi);
            il.Emit(OpCodes.Conv_U);
            il.Emit(OpCodes.Ldloc_0);
            il.Emit(OpCodes.Conv_U);
            il.Emit(OpCodes.Sub);
            // Subtract HeaderSize
            il.Emit(OpCodes.Ldc_I4, HeaderSize);
            il.Emit(OpCodes.Sub);
            // Convert to long
            il.Emit(OpCodes.Conv_I8);
            il.Emit(OpCodes.Ret);

            Func<long> offsetHelper = (Func<long>)method.CreateDelegate(typeof(Func<long>));
            long offset = offsetHelper();
            return offsetHelper();
        }

        public T GetFieldVolatile<T>(object o, long offset)
        {
            if (o is TypeWrapper w)
                return staticAccessor.GetFieldVolatile<T>(o, offset);
            else
                return objectAccessor.GetFieldVolatile<T>(o, offset);
        }

        public void PutFieldVolatile<T>(object o, long offset, T value)
        {
            if (o is TypeWrapper w)
                staticAccessor.PutFieldVolatile(o, offset, value);
            else
                objectAccessor.PutFieldVolatile(o, offset, value);
        }

        public bool CompareAndSwapField<T>(object o, long offset, T expected, T value)
        {
            if (o is TypeWrapper w)
                return staticAccessor.CompareAndSwapField(o, offset, expected, value);
            else
                return objectAccessor.CompareAndSwapField(o, offset, expected, value);
        }
    }
}
