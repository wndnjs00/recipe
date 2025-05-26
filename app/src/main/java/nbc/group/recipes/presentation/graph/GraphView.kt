package nbc.group.recipes.presentation.graph

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
//import nbc.group.recipes.GlideApp
import nbc.group.recipes.R
import nbc.group.recipes.data.model.dto.Item
import nbc.group.recipes.data.model.dto.Recipe
import nbc.group.recipes.data.model.entity.RecipeEntity
import nbc.group.recipes.getRecipeImageUrl
import nbc.group.recipes.presentation.fragment.FROM_FIREBASE
import kotlin.math.sin
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class GraphView : View {

    companion object {
        const val MIN_ZOOM = 0.1f
        const val MAX_ZOOM = 5f
    }

    private val nodes = mutableListOf<Node<Any>>()
    private val edges = mutableListOf<Edge>()

    private var targetNode = -1

    private var pointF: PointF = PointF()
    private var detector: ScaleGestureDetector? = null
    private var longClickDetector: GestureDetector? = null
    private var scaleFactor = 1f

    private var touchX: Float = 0f
    private var touchY: Float = 0f

    private var zooming: Boolean = false

    private var startX = 0f
    private var startY = 0f
    private var translateX = 0f
    private var translateY = 0f
    private var previousTranslateX = 0f
    private var previousTranslateY = 0f

    private val nodeSize = 120

    private val centerX = Resources.getSystem().displayMetrics.widthPixels / 2f
    private val centerY = Resources.getSystem().displayMetrics.heightPixels / 2f

    private val nodePaint = Paint()
    private val edgePaint = Paint()
    private val textPaint = Paint()
    private val borderPaint = Paint()
    private val indicatorPaint = Paint()

    private var getData: ((String, Int) -> Unit)? = null
    private var singleTapUp: ((RecipeEntity) -> Unit)? = null

    init {
        detector = ScaleGestureDetector(context, ScaleListener())
        longClickDetector = GestureDetector(context, LongClickListener())

        borderPaint.color = ContextCompat.getColor(context, R.color.green1)
        indicatorPaint.color = Color.parseColor("#2214870C")
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setValue(
        nodes: List<Node<Any>>,
        edges: List<Edge>,
    ) {
        this.nodes.clear()
        this.edges.clear()
        this.nodes.addAll(nodes)
        this.edges.addAll(edges)
    }

    fun getNodes() = nodes
    fun getEdges() = edges
    fun setNodes(nodes: List<Node<Any>>) {
        this.nodes.clear()
        this.nodes.addAll(nodes)
    }
    fun setEdges(edges: List<Edge>) {
        this.edges.clear()
        this.edges.addAll(edges)
    }


    suspend fun addNode(data: Any, connectedIndex: Int?) {

        val currentIndex = nodes.size

        nodes.add(
            Node(
                x = centerX + Random.nextInt(-20, 20),
                y = centerY + Random.nextInt(-20, 20),
                dx = 0f,
                dy = 0f,
                size = 60f,
                weight = 1f,
                bitmap = null,
                data = data
            )
        )

        connectedIndex?.let {
            edges.add(
                Edge(it, nodes.size - 1)
            )
        }

        // todo: 해당 부분 함수로 작성
        if(data is RecipeEntity) {
            if(data.from == FROM_FIREBASE) {
                val ralph = Firebase.storage.reference.child(data.recipeImg)
                Log.e("URGENT_TAG", "onResourceReady: $ralph", )
                Glide.with(this)
                    .asBitmap()
                    .load(Firebase.storage.reference.child(data.recipeImg))
                    .into(object: CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                            nodes[currentIndex] = nodes[currentIndex].copy(
                                bitmap = adjustImage(resource, nodeSize, nodeSize)
                            )
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {

                        }
                    })
            } else {
                Glide.with(this)
                    .asBitmap()
                    .load(data.recipeImg)
                    .into(object: CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            nodes[currentIndex] = nodes[currentIndex].copy(
                                bitmap = adjustImage(resource, nodeSize, nodeSize)
                            )
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {

                        }
                    })
            }
        } else {
            Glide.with(this)
                .asBitmap()
                .load(
                    when(data) {
                        is Item -> data.imgUrl
                        else -> throw Exception()
                    }
                )
                .into(object: CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        nodes[currentIndex] = nodes[currentIndex].copy(
                            bitmap = adjustImage(resource, nodeSize, nodeSize)
                        )
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }
                })
        }
//
//        Glide.with(this)
//            .asBitmap()
//            .load(
//                when(data) {
//                    is Item -> data.imgUrl
//                    is RecipeEntity -> data.recipeImg
//                    else -> throw Exception()
//                }
//            )
//            .into(object: CustomTarget<Bitmap>() {
//                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                    nodes[currentIndex] = nodes[currentIndex].copy(
//                        bitmap = adjustImage(resource, nodeSize, nodeSize)
//                    )
//                }
//
//                override fun onLoadCleared(placeholder: Drawable?) {
//
//                }
//            })

//        Glide.with(this)
//            .asBitmap()
//            .load(
//                when(data) {
//                    is Item -> data.imgUrl
//                    is RecipeEntity -> data.recipeImg
//                    else -> throw Exception()
//                }
//            )
//            .into(object : CustomTarget<Bitmap>() {
//                override fun onResourceReady(
//                    resource: Bitmap,
//                    transition: Transition<in Bitmap>?,
//                ) {
//                    nodes.add(
//                        Node(
//                            x = centerX + Random.nextInt(-20, 20),
//                            y = centerY + Random.nextInt(-20, 20),
//                            dx = 0f,
//                            dy = 0f,
//                            size = 60f,
//                            weight = 1f,
//                            bitmap = adjustImage(resource, nodeSize, nodeSize),
//                            data = data
//                        )
//                    )
//
//                    invalidate()
//                }
//
//                override fun onLoadCleared(placeholder: Drawable?) {
//
//                }
//            })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        this.touchX = event.x
        this.touchY = event.y

        when (event.action) {


            MotionEvent.ACTION_DOWN -> {
                if (targetNode == -1) {
                    for (i in 0 until nodes.size) {
                        if (nodes[i].x + nodes[i].size / 2 >= (touchX - translateX) / scaleFactor &&
                            nodes[i].x <= (touchX - translateX) / scaleFactor &&
                            nodes[i].y + nodes[i].size / 2 >= (touchY - translateY) / scaleFactor &&
                            nodes[i].y <= (touchY - translateY) / scaleFactor
                        ) {
                            targetNode = i
                        }
                    }
                    if (targetNode == -1) {
                        startX = event.x - previousTranslateX
                        startY = event.y - previousTranslateY
                    }

                } else {
                    nodes[targetNode] = nodes[targetNode].copy(
                        x = ((touchX - translateX) / scaleFactor - nodes[targetNode].size / 2)
                    )
                    nodes[targetNode] = nodes[targetNode].copy(
                        y = ((touchY - translateY) / scaleFactor - nodes[targetNode].size / 2)
                    )
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!zooming) {

                    if (targetNode == -1) {
                        translateX = event.x - startX
                        translateY = event.y - startY

                    } else {
                        nodes[targetNode] = nodes[targetNode].copy(
                            x = ((touchX - translateX) / scaleFactor - nodes[targetNode].size / 2)
                        )
                        nodes[targetNode] = nodes[targetNode].copy(
                            y = ((touchY - translateY) / scaleFactor - nodes[targetNode].size / 2)
                        )
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                zooming = false
                targetNode = -1

                previousTranslateX = translateX
                previousTranslateY = translateY
            }

            MotionEvent.ACTION_POINTER_UP -> {
                zooming = false

                previousTranslateX = translateX
                previousTranslateY = translateY
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                midPoint(pointF, event)
                zooming = true
                targetNode = -1
            }

            MotionEvent.ACTION_CANCEL -> {
                zooming = false
                targetNode = -1

                previousTranslateX = translateX
                previousTranslateY = translateY
            }
        }
        detector!!.onTouchEvent(event)
        longClickDetector!!.onTouchEvent(event)
        return true
    }


    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        if (nodes.size == 0) return

        canvas.scale(scaleFactor, scaleFactor, pointF.x, pointF.y)
        canvas.translate(translateX / scaleFactor, translateY / scaleFactor)

        val opTime = measureTimeMillis {
            operate(nodes, edges, targetNode)
        }

        // Log.e("TAG", "dispatchDraw: opTime: $opTime")

        edges.forEach { edge ->
            val toNode = nodes[edge.to]
            val fromNode = nodes[edge.from]
            canvas.drawLine(toNode.x, toNode.y, fromNode.x, fromNode.y, edgePaint)
        }

        nodes.forEachIndexed { index, node ->

            val tempData = node.data
            val name = when (tempData) {
                is Item -> tempData.cntntsSj
                is RecipeEntity -> tempData.recipeName
                else -> ""
            }?: ""

            canvas.drawCircle(node.x, node.y, 60f, nodePaint)

            canvas.drawRect(
                node.x - 62f,
                node.y - 62f,
                node.x + 62f,
                node.y + 62f,
                borderPaint
            )
            node.bitmap?.let {
                canvas.drawBitmap(it, node.x - 60f, node.y - 60f, nodePaint)
            }

            canvas.drawText(
                name,
                node.x - 60f,
                node.y + 60f + 20f,
                textPaint
            )

            if(index == targetNode) {
                canvas.drawCircle(node.x, node.y, 120f, indicatorPaint)
            }
        }

        invalidate()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM))
            return true
        }
    }

    private inner class LongClickListener: GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            Log.e("URGENT_TAG", "onSingleTapUp: ", )

            val targetIndex = findTargetNode(e)
            if(targetIndex != -1) {
                Log.e("URGENT_TAG", "onSingleTapUp: $targetIndex", )
                val temp = nodes[targetIndex].data
                if(temp is RecipeEntity) {
                    singleTapUp?.let {
                        it(temp)
                    }
                }
            }

            return true
        }
        override fun onLongPress(e: MotionEvent) {
            if(targetNode != -1) {
                val temp = nodes[targetNode].data
                if(temp is Item) {
                    getData?.let {
                        it(temp.cntntsSj?: "", targetNode)
                    }
                } else if(temp is RecipeEntity) {

                }
            }
        }
    }

    private fun midPoint(pointF: PointF, motionEvent: MotionEvent) {
        val x = motionEvent.getX(0) + motionEvent.getX(1)
        val y = motionEvent.getY(0) + motionEvent.getY(1)
        pointF.set(x / 2, y / 2)
    }

    fun registerLongClickListener(lambda: (String, Int) -> Unit) {
        getData = lambda
    }

    fun registerSingleTapUpListener(lambda: (RecipeEntity) -> Unit) {
        singleTapUp = lambda
    }

    fun findTargetNode(e: MotionEvent): Int {
        var result = -1

        val x = e.x
        val y = e.y

        for (i in 0 until nodes.size) {
            if (nodes[i].x + nodes[i].size / 2 >= (x - translateX) / scaleFactor &&
                nodes[i].x <= (x - translateX) / scaleFactor &&
                nodes[i].y + nodes[i].size / 2 >= (y - translateY) / scaleFactor &&
                nodes[i].y <= (y - translateY) / scaleFactor
            ) {
                result = i
            }
        }
        return result
    }
}