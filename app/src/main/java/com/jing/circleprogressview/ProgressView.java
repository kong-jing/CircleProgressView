package com.jing.circleprogressview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by kongjing on 2016/8/18.
 * 实现android自定义进度条渐变圆形圈的代码
 * TODO 对于圈的整体大小，取决于布局的大小
 * TODO 画刻度线
 */
public class ProgressView extends View {
  private static final String TAG = "ProgressView";
  private int mSection = 4; // 值域（mMax-mMin）等分份数
  private String[] mTexts;//刻度数组
  private int mMin = 0; // 最小值 刻度数
  private int mMax = 100; // 最大值 刻度数
  private int mStartAngle = 150; // 起始角度
  private int mSweepAngle = 240; // 绘制角度
  private float mCenterX, mCenterY; // 圆心坐标
  private int mRadius; // 扇形半径
  private int mPadding;
  private int mStrokeWidth; // 画笔宽度
  private int mLength1; // 长刻度的相对圆弧的长度
  private int mLength2; // 刻度读数顶部的相对圆弧的长度
  private String mHeaderText = "100"; // 表头
  private Rect mRectText;
  //分段颜色,修改圆圈的渐变色
  public int[] SECTION_COLORS = { Color.RED, Color.YELLOW, Color.GREEN };
  private static final String[] ALARM_LEVEL = { "通过", "未通过", "中危", "高危" };
  private float maxCount;
  private float currentCount;
  private double score;
  private String crrentLevel;
  private Paint mPaint;//圈
  private Paint mTextPaint;//文本
  private Paint mTextPaint_xs;//文本2
  private Paint mPaintBg;//圆弧背景
  private Paint degreePaint;//外部刻度
  private Paint pointerPaint;//指针
  private int mWidth, mHeight;
  private float currentAngleLength = 0;//圆弧的长度
  private int mTextSize;//设置一个全局使用的圈内字体大小
  Context mContext;

  int inner_circle, outter_circle, textheight;
  int xsd_textsize;//相似度字体大小
  int screenWidth, screenHeight;
  int strokeWidth, strokeWidthOutter;//进度条的粗细

  public ProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    this.mContext = context;
    init(context);
    //设置app属性
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressView, defStyleAttr, 0);
    mTextSize = (int) a.getDimension(R.styleable.ProgressView_pv_textsize, 14);
    inner_circle = (int) a.getDimension(R.styleable.ProgressView_pv_inner_circle, 20);//用于圆弧的宽度
    outter_circle = (int) a.getDimension(R.styleable.ProgressView_pv_outter_circle, 21);//用于外圈圆弧的宽度
    textheight = (int) a.getDimension(R.styleable.ProgressView_pv_text_height, 16);
    xsd_textsize = (int) a.getDimension(R.styleable.ProgressView_pv_xsd_textsize, 40);
    strokeWidth = (int) a.getDimension(R.styleable.ProgressView_pv_stroke_width, 30);
    strokeWidthOutter = (int) a.getDimension(R.styleable.ProgressView_pv_stroke_width_outter, 20);

    //WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    //screenWidth = wm.getDefaultDisplay().getWidth();
    //screenHeight = wm.getDefaultDisplay().getHeight();
    screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
  }

  public ProgressView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
    this.mContext = context;
  }

  public ProgressView(Context context) {
    this(context, null);
    this.mContext = context;
  }

  private void init(Context context) {
    mPaint = new Paint();
    mTextPaint = new Paint();
    mPaintBg = new Paint();
    mTextPaint_xs = new Paint();
    //外部刻度线
    degreePaint = new Paint();
    //指针
    pointerPaint = new Paint();
    mTexts = new String[mSection + 1];//需要显示的刻度数有几个
    for (int i = 0; i < mTexts.length; i++) {
      int n = (mMax - mMin) / mSection;
      mTexts[i] = String.valueOf(mMin + i * n);
    }
    mStrokeWidth = dipToPx(3);
    mLength1 = dipToPx(8) + mStrokeWidth;
    //mLength2 = mLength1 - dipToPx(8);
    mRectText = new Rect();
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    initPaint();
    //抗锯齿
    canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));

    RectF rectBlackBg = new RectF(inner_circle, inner_circle, mWidth - inner_circle, mHeight - inner_circle);//内圈圆弧
    //canvas.drawArc(rectBlackBg, 150, 240, true,
    //    mPaint);//oval：圆弧所在的椭圆对象。startAngle：圆弧的起始角度。sweepAngle：圆弧的角度。useCenter：是否显示半径连线，true表示显示圆弧与圆心的半径连线，false表示不显示。paint：绘制时所使用的画笔。
    RectF rectbg = new RectF(outter_circle, outter_circle, mWidth - outter_circle,
        mHeight - outter_circle);//我想要给圆弧加一个灰色背景，外圈圆弧

    mPaintBg.setColor(Color.rgb(97, 155, 186));//设置背景圆弧画笔

    //为了画刻度数，我也是拼了
    degreePaint.setTextSize(dipToPx(16));
    degreePaint.setStyle(Paint.Style.FILL);

    float angle = mSweepAngle * 1f / mSection;
    float α;
    float[] p;
    angle = mSweepAngle * 1f / mSection;
    for (int i = 0; i <= mSection; i++) {
      α = mStartAngle + angle * i;
      p = getCoordinatePoint(mRadius - mLength2, α);
      if (α % 360 > 135 && α % 360 < 225) {
        degreePaint.setTextAlign(Paint.Align.LEFT);
      } else if ((α % 360 >= 0 && α % 360 < 45) || (α % 360 > 315 && α % 360 <= 360)) {
        degreePaint.setTextAlign(Paint.Align.RIGHT);
      } else {
        degreePaint.setTextAlign(Paint.Align.CENTER);
      }
      degreePaint.getTextBounds(mHeaderText, 0, mTexts[i].length(), mRectText);
      int txtH = mRectText.height();
      //if (i <= 1 || i >= mSection - 1) {
      //  canvas.drawText(mTexts[i], p[0] + txtH / 3, p[1] + txtH / 2 +6, degreePaint);
      //} else if (i == 3) {
      //  canvas.drawText(mTexts[i], p[0] + txtH / 2, p[1] + txtH, degreePaint);
      //} else if (i == mSection - 3) {
      //  canvas.drawText(mTexts[i], p[0] - txtH / 2, p[1] + txtH, degreePaint);
      //} else {
      //  canvas.drawText(mTexts[i], p[0], p[1] + txtH, degreePaint);
      //}

      switch (i){
        case 0:
          canvas.drawText(mTexts[i], p[0] + txtH / 2, p[1] + txtH /3 , degreePaint);
          break;
        case 1:
          canvas.drawText(mTexts[i], p[0] - txtH / 3 , p[1] + txtH / 2 +6, degreePaint);
          break;
        case 2:
          canvas.drawText(mTexts[i], p[0] - txtH /3, p[1] + txtH  +10, degreePaint);
          break;
        case 3:
          canvas.drawText(mTexts[i], p[0] + txtH / 3, p[1] + txtH / 2 +6, degreePaint);
          break;
        case 4:
          canvas.drawText(mTexts[i], p[0] + txtH / 3, p[1] + txtH / 2 +2, degreePaint);
          break;
        default:
          canvas.drawText(mTexts[i], p[0], p[1] + txtH, degreePaint);
          break;
      }

    }
    //为了画一个指针
    RectF rectFPointer = new RectF(mCenterX - (int) (mRadius / 3f / 2 / 2),
        mCenterY - (int) (mRadius / 3f / 2 / 2), mCenterX + (int) (mRadius / 3f / 2 / 2), mCenterY + (int) (mRadius / 3f / 2 / 2));
    int anglePointer = (int) (3.6*currentAngleLength  + mStartAngle);
    //指针的定点坐标
    int[] peakPoint = getPointFromAngleAndRadius(anglePointer, (int) (mRadius / 7f ));
    //顶点朝上，左侧的底部点的坐标
    int[] bottomLeft = getPointFromAngleAndRadius(anglePointer - 90, (int) (mRadius / 3f / 2 / 2 ));
    //顶点朝上，右侧的底部点的坐标
    int[] bottomRight = getPointFromAngleAndRadius(anglePointer + 90, (int) (mRadius / 3f / 2 / 2 ));

    Path path = new Path();
    int[] colors = new int[]{Color.rgb(97, 155, 186), Color.rgb(55, 145, 189), Color.rgb(88, 180, 223) };
    Shader shader =
        new LinearGradient(peakPoint[0], peakPoint[1], bottomLeft[0], bottomLeft[1],
            Color.rgb(99, 188, 230), Color.rgb(93, 153, 183), Shader.TileMode.CLAMP);
    //Shader shader = new RadialGradient(mCenterX, mCenterY, (int) (mRadius / 3f / 2 / 2 ), Color.rgb(93, 153, 183),
    //    Color.rgb(99, 188, 230), Shader.TileMode.CLAMP);
    pointerPaint.setColor(Color.rgb(97, 155, 186));
    pointerPaint.setStrokeWidth(1);
    pointerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    pointerPaint.setDither(true);
    pointerPaint.setAntiAlias(true);
    pointerPaint.setShader(shader);
    path.reset();
    path.setFillType(Path.FillType.EVEN_ODD);
    path.moveTo(mCenterX, mCenterY);
    path.lineTo(peakPoint[0], peakPoint[1]);
    path.lineTo(bottomLeft[0], bottomLeft[1]);
    path.close();
    canvas.drawPath(path, pointerPaint);
    canvas.drawArc(rectFPointer, anglePointer - 190, 100, true, pointerPaint);


    path.reset();
    path.moveTo(mCenterX, mCenterY);
    path.lineTo(peakPoint[0], peakPoint[1]);
    path.lineTo(bottomRight[0], bottomRight[1]);
    path.close();
    canvas.drawPath(path, pointerPaint);
    canvas.drawArc(rectFPointer, anglePointer + 90, 100, true, pointerPaint);


    canvas.drawArc(rectbg, 150, 240, false, mPaintBg);

    //外部圆圈中的的画笔
    mPaint.setColor(Color.rgb(54, 176, 234));
    mTextPaint.setTextSize(mTextSize);//设置圆圈内字体的大小
    mTextPaint.setColor(Color.rgb(54, 176, 234));//设置圆圈内字体颜色
    canvas.drawText(score + "%" , mWidth *50/ 100, mHeight*35 / 100 + textheight, mTextPaint);//分数

    mTextPaint_xs.setTextSize(xsd_textsize);//设置圆圈内字体的大小
    mTextPaint_xs.setColor(Color.rgb(54, 176, 234));//设置圆圈内字体颜色
    canvas.drawText("相似度" , mWidth * 35 / 100 , mHeight * 75 / 100 , mTextPaint_xs);

    if (crrentLevel != null) {
      canvas.drawText(crrentLevel, mWidth / 2, mHeight / 2 + 40, mTextPaint);
    }
    //float section = currentCount / maxCount;
    //if (section <= 1.0f / 3.0f) {
    //  if (section != 0.0f) {
    //    mPaint.setColor(SECTION_COLORS[0]);
    //  } else {
    //    mPaint.setColor(Color.TRANSPARENT);
    //  }
    //} else {
    //  int count = (section <= 1.0f / 3.0f * 2) ? 2 : 3;
    //  int[] colors = new int[count];
    //  System.arraycopy(SECTION_COLORS, 0, colors, 0, count);
    //  float[] positions = new float[count];
    //  if (count == 2) {
    //    positions[0] = 0.0f;
    //    positions[1] = 1.0f - positions[0];
    //  } else {
    //    positions[0] = 0.0f;
    //    positions[1] = (maxCount / 3) / currentCount;
    //    positions[2] = 1.0f - positions[0] * 2;
    //  }
    //  positions[positions.length - 1] = 1.0f;
    //  LinearGradient shader =
    //      new LinearGradient(3, 3, (mWidth - 3) * section, mHeight - 3, colors, null,
    //          Shader.TileMode.MIRROR);
    //  mPaint.setShader(shader);
    //}

    mPaint.setShadowLayer(30, 5, 90, Color.BLACK);
    mPaint.setDither(true);
    canvas.drawArc(rectBlackBg, 150, currentAngleLength * 3.6f, false, mPaint);
  }

  private void initPaint() {
    mPaint.setAntiAlias(true);
    mPaint.setStrokeWidth((float) strokeWidth);
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setStrokeCap(Paint.Cap.SQUARE);//设置形状
    mPaint.setColor(Color.TRANSPARENT);
    mTextPaint.setAntiAlias(true);
    mTextPaint.setStrokeWidth((float) 0.5);
    mTextPaint.setTextAlign(Paint.Align.CENTER);
    mTextPaint.setTextSize(20);
    mTextPaint.setColor(Color.rgb(81, 89, 116));
    mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
    mPaintBg.setAntiAlias(true);
    mPaintBg.setStrokeWidth((float) strokeWidthOutter);
    mPaintBg.setStyle(Paint.Style.STROKE);
    mPaintBg.setStrokeCap(Paint.Cap.SQUARE);
    mPaintBg.setColor(Color.TRANSPARENT);
    degreePaint.setColor(Color.parseColor("#999999"));//外部刻度

  }

  private int dipToPx(int dip) {
    float scale = getContext().getResources().getDisplayMetrics().density;
    return (int) (dip * scale + 0.5f * (dip >= 0 ? 1 : -1));
  }

  public double getScore() {
    return score;
  }

  public String getCrrentLevel() {
    return crrentLevel;
  }

  public void setCrrentLevel(String crrentLevel) {
    this.crrentLevel = crrentLevel;
  }

  public float getMaxCount() {
    return maxCount;
  }

  public float getCurrentCount() {
    return currentCount;
  }

  /**
   * 设置分数
   */
  public void setScore(double score) {
    this.score = score;
    //        if (score == 100) {
    //            this.crrentLevel = ALARM_LEVEL[0];
    //        } else if (score >= 70 && score < 100) {
    //            this.crrentLevel = ALARM_LEVEL[1];
    //        } else if (score >= 30 && score < 70) {
    //            this.crrentLevel = ALARM_LEVEL[2];
    //        } else {
    //            this.crrentLevel = ALARM_LEVEL[3];
    //        }
    invalidate();
  }

  /***
   * 设置最大的进度值
   * 在使用的时候要优先设置
   * @param maxCount
   */
  public void setMaxCount(float maxCount) {
    this.maxCount = maxCount * 100f;
  }

  /***
   * 设置当前的进度值
   * @param currentCount
   */
  public void setCurrentCount(float currentCount) {
    this.currentCount = currentCount * 1f > maxCount ? maxCount : currentCount * 1f;
    setAnimation(0, this.currentCount, 1000);
    //invalidate();
  }

  public void setCurrentTextSize(int textSize) {
    this.mTextSize = textSize;
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
    if (widthSpecMode == MeasureSpec.EXACTLY || widthSpecMode == MeasureSpec.AT_MOST) {
      mWidth = widthSpecSize;
    } else {
      mWidth = 0;
    }
    if (heightSpecMode == MeasureSpec.AT_MOST || heightSpecMode == MeasureSpec.UNSPECIFIED) {
      mHeight = dipToPx(15);
    } else {
      mHeight = heightSpecSize;
    }
    setMeasuredDimension(mWidth, mHeight);
    mCenterX = mCenterY = getMeasuredWidth() / 2f;//设置圆心的坐标
    int width = resolveSize(dipToPx(260), widthMeasureSpec);

    mPadding = Math.max(
        Math.max(getPaddingLeft(), getPaddingTop()),
        Math.max(getPaddingRight(), getPaddingBottom())
    );
    setPadding(mPadding, mPadding, mPadding, mPadding);

    mRadius = (width - mPadding * 2 - mStrokeWidth * 2) / 2;//设置扇形半径

  }

  /**
   * 为进度设置动画
   * ValueAnimator是整个属性动画机制当中最核心的一个类，属性动画的运行机制是通过不断地对值进行操作来实现的，
   * 而初始值和结束值之间的动画过渡就是由ValueAnimator这个类来负责计算的。
   * 它的内部使用一种时间循环的机制来计算值与值之间的动画过渡，
   * 我们只需要将初始值和结束值提供给ValueAnimator，并且告诉它动画所需运行的时长，
   * 那么ValueAnimator就会自动帮我们完成从初始值平滑地过渡到结束值这样的效果。
   */
  private void setAnimation(float last, float current, int length) {
    ValueAnimator progressAnimator = ValueAnimator.ofFloat(last, current);
    progressAnimator.setDuration(length);
    progressAnimator.setTarget(currentAngleLength);
    progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override public void onAnimationUpdate(ValueAnimator animation) {
        currentAngleLength = (float) animation.getAnimatedValue();
        // Log.e("test",currentAngleLength+"");
        invalidate();
      }
    });
    progressAnimator.start();
  }


  public float[] getCoordinatePoint(int radius, float angle) {
    float[] point = new float[2];

    double arcAngle = Math.toRadians(angle); //将角度转换为弧度
    if (angle < 90) {
      point[0] = (float) (mCenterX + Math.cos(arcAngle) * radius);
      point[1] = (float) (mCenterY + Math.sin(arcAngle) * radius);
    } else if (angle == 90) {
      point[0] = mCenterX;
      point[1] = mCenterY + radius;
    } else if (angle > 90 && angle < 180) {
      arcAngle = Math.PI * (180 - angle) / 180.0;
      point[0] = (float) (mCenterX - Math.cos(arcAngle) * radius);
      point[1] = (float) (mCenterY + Math.sin(arcAngle) * radius);
    } else if (angle == 180) {
      point[0] = mCenterX - radius;
      point[1] = mCenterY;
    } else if (angle > 180 && angle < 270) {
      arcAngle = Math.PI * (angle - 180) / 180.0;
      point[0] = (float) (mCenterX - Math.cos(arcAngle) * radius);
      point[1] = (float) (mCenterY - Math.sin(arcAngle) * radius);
    } else if (angle == 270) {
      point[0] = mCenterX;
      point[1] = mCenterY - radius;
    } else {
      arcAngle = Math.PI * (360 - angle) / 180.0;
      point[0] = (float) (mCenterX + Math.cos(arcAngle) * radius);
      point[1] = (float) (mCenterY - Math.sin(arcAngle) * radius);
    }

    return point;
  }

  /**
   * 根据角度和半径，求一个点的坐标
   *
   * @param angle
   * @param radius
   * @return
   */
  private int[] getPointFromAngleAndRadius(int angle, int radius) {
    double x = radius * Math.cos(angle * Math.PI / 180) + mCenterX;
    double y = radius * Math.sin(angle * Math.PI / 180) + mCenterY;
    return new int[]{(int) x, (int) y};
  }

  public void setBackgroundColor(int[] color){
    SECTION_COLORS = new int[]{color[0], color[1], color[2]};
  }


}
