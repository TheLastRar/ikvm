using System;

using IKVM.Internal;

namespace IKVM.Java.Externs.sun.misc
{
    internal class UnsafeTypeWrapper : IUnsafeFieldAccessor
    {
        /// <summary>
        /// Implements the logic to get a field by offset.
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="o"></param>
        /// <param name="offset"></param>
        /// <returns></returns>
        /// <exception cref="global::java.lang.InternalError"></exception>
        public T GetField<T>(object o, long offset)
        {
#if FIRST_PASS
            throw new NotImplementedException();
#else
            var f = FieldWrapper.FromCookie((IntPtr)offset);
            if (o is TypeWrapper w)
            {
                if (w != f.DeclaringType)
                    throw new global::java.lang.IllegalArgumentException();

                return f.UnsafeGetValue<T>(null);
            }

            return f.UnsafeGetValue<T>(o);
#endif
        }

        /// <summary>
        /// Implements the logic to set a field by offset.
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="o"></param>
        /// <param name="offset"></param>
        /// <param name="value"></param>
        /// <exception cref="global::java.lang.InternalError"></exception>
        public void PutField<T>(object o, long offset, T value)
        {
#if FIRST_PASS
            throw new NotImplementedException();
#else
            var f = FieldWrapper.FromCookie((IntPtr)offset);
            if (o is TypeWrapper w)
            {
                if (w != f.DeclaringType)
                    throw new global::java.lang.IllegalArgumentException();

                f.UnsafeSetValue<T>(null, value);
            }

            f.UnsafeSetValue<T>(o, value);
#endif
        }

        /// <summary>
        /// Implementation of native method 'staticFieldBase'.
        /// </summary>
        /// <param name="self"></param>
        /// <param name="f"></param>
        /// <returns></returns>
        public object StaticFieldBase(global::java.lang.reflect.Field f)
        {
            var w = FieldWrapper.FromField(f);
            if (w.IsStatic == false)
                throw new global::java.lang.IllegalArgumentException();

            return w.DeclaringType;
        }

        /// <summary>
        /// Implementation of native method 'staticFieldOffset'.
        /// </summary>
        /// <param name="self"></param>
        /// <param name="f"></param>
        /// <returns></returns>
        public long StaticFieldOffset(global::java.lang.reflect.Field f)
        {
            var w = FieldWrapper.FromField(f);
            if (w.IsStatic == false)
                throw new global::java.lang.IllegalArgumentException();

            return (long)w.Cookie;
        }

        /// <summary>
        /// Implementation of native method 'objectFieldOffset'.
        /// </summary>
        /// <param name="self"></param>
        /// <param name="f"></param>
        /// <returns></returns>
        public long ObjectFieldOffset(global::java.lang.reflect.Field f)
        {
            var w = FieldWrapper.FromField(f);
            if (w.IsStatic)
                throw new global::java.lang.IllegalArgumentException();

            return (long)w.Cookie;
        }

        /// <summary>
        /// Implements the logic to get a field by offset using volatile.
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="o"></param>
        /// <param name="offset"></param>
        /// <returns></returns>
        /// <exception cref="global::java.lang.InternalError"></exception>
        public T GetFieldVolatile<T>(object o, long offset)
        {
#if FIRST_PASS
            throw new NotImplementedException();
#else
            try
            {
                var f = FieldWrapper.FromCookie((IntPtr)offset);
                if (o is TypeWrapper w)
                {
                    if (w != f.DeclaringType)
                        throw new global::java.lang.IllegalArgumentException();

                    return f.UnsafeVolatileGet<T>(null);
                }

                return f.UnsafeVolatileGet<T>(o);
            }
            catch (Exception e)
            {
                throw new global::java.lang.InternalError(e);
            }
#endif
        }

        /// <summary>
        /// Implements the logic to set a field by offset using volatile.
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="o"></param>
        /// <param name="offset"></param>
        /// <param name="value"></param>
        /// <exception cref="global::java.lang.InternalError"></exception>
        public void PutFieldVolatile<T>(object o, long offset, T value)
        {
#if FIRST_PASS
            throw new NotImplementedException();
#else
            try
            {
                var f = FieldWrapper.FromCookie((IntPtr)offset);
                if (o is TypeWrapper w)
                {
                    if (w != f.DeclaringType)
                        throw new global::java.lang.IllegalArgumentException();

                    f.UnsafeVolatileSet<T>(null, value);
                }

                f.UnsafeVolatileSet<T>(o, value);
            }
            catch (Exception e)
            {
                throw new global::java.lang.InternalError(e);
            }
#endif
        }

        /// <summary>
        /// Implements the logic to compare and swap an object.
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="o"></param>
        /// <param name="offset"></param>
        /// <param name="value"></param>
        /// <param name="expected"></param>
        /// <returns></returns>
        /// <exception cref="global::java.lang.InternalError"></exception>
        public bool CompareAndSwapField<T>(object o, long offset, T expected, T value)
        {
#if FIRST_PASS
            throw new NotImplementedException();
#else
            try
            {
                return FieldWrapper.FromCookie((IntPtr)offset).UnsafeCompareAndSwap(o, expected, value);
            }
            catch (Exception e)
            {
                throw new global::java.lang.InternalError(e);
            }
#endif
        }
    }
}
