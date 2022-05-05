1、添加maven依赖

# implementation 'com.github.jt0521:crash:Tag'

2、Application 中初始化

# CrashHandler.init(this,debug);

3、显示日志列表

# ShowLogDialog.show(Context context)//立即显示

# ShowLogDialog.showLongPress(Window window)//长按超5秒显示

# ShowLogDialog.showContinuousClick(Window window)   //快速点击超8次显示