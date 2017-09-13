package space.zhupeng.easycamera;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by zhupeng on 2017/9/12.
 */

public class CameraInvocationHandler implements InvocationHandler {

    private CameraApi mDelegate;
    private CheckPermissionListener mCheckListener;

    private CameraInvocationHandler(CheckPermissionListener listener) {
        this.mCheckListener = listener;
    }

    public static CameraInvocationHandler of(CheckPermissionListener listener) {
        return new CameraInvocationHandler(listener);
    }

    /**
     * 绑定委托对象并返回一个代理类
     *
     * @param delegate
     * @return
     */
    public CameraApi bind(CameraApi delegate) {
        this.mDelegate = delegate;
        return (CameraApi) Proxy.newProxyInstance(delegate.getClass().getClassLoader(), delegate.getClass().getInterfaces(), this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean isGranted = mCheckListener.isPermissionGranted();
        if (proxy instanceof CameraApi) {
            if (!isGranted) {
//                ((CameraApi) proxy).requestPermission();
            }
        }
        return method.invoke(mDelegate, args);
    }
}
