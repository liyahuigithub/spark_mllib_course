package classificationmodel

import classificationmodel.pipline.{DecisionTreePipeline, LogisticRegressionPipeline, NaiveBayesPipeline}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.DataType
import org.apache.spark.sql.{DataFrame, SQLContext}


/**
  * Created by manpreet.singh on 25/04/16.
  */
object StumbleUponExecutor {
  @transient lazy val logger = Logger.getLogger(getClass.getName)

  def main(args: Array[String]) {

    logger.setLevel(Level.OFF)


    val conf: SparkConf = SparkCommonUtils.createSparkConf("StumbleUpon")
    val sc = new SparkContext(conf)
    sc.setLogLevel("ERROR")

    // 创建 sql 上下文 create sql context
    val sqlContext = new SQLContext(sc)

    // 获取数据 get dataframe
    val df = sqlContext.read.format("com.databricks.spark.csv").option("delimiter", "\t").option("header", "true")
      .option("inferSchema", "true").load("/home/rjxy/IdeaProjects/spark/spark_mllib_course/src/main/resources/stumbleupon/train.tsv")

    // 预处理 pre-processing
    df.registerTempTable("StumbleUpon")
    //打印 格式
    df.printSchema()
    sqlContext.sql("SELECT * FROM StumbleUpon WHERE alchemy_category = '?'").show()


    //将“4 到 n”列的类型转换为双精度 convert type of '4 to n' columns to double
    val df1: DataFrame = df.withColumn("avglinksize", df("avglinksize").cast("double"))
      .withColumn("commonlinkratio_1", df("commonlinkratio_1").cast("double"))
      .withColumn("commonlinkratio_2", df("commonlinkratio_2").cast("double"))
      .withColumn("commonlinkratio_3", df("commonlinkratio_3").cast("double"))
      .withColumn("commonlinkratio_4", df("commonlinkratio_4").cast("double"))
      .withColumn("compression_ratio", df("compression_ratio").cast("double"))
      .withColumn("embed_ratio", df("embed_ratio").cast("double"))
      .withColumn("framebased", df("framebased").cast("double"))
      .withColumn("frameTagRatio", df("frameTagRatio").cast("double"))
      .withColumn("hasDomainLink", df("hasDomainLink").cast("double"))
      .withColumn("html_ratio", df("html_ratio").cast("double"))
      .withColumn("image_ratio", df("image_ratio").cast("double"))
      .withColumn("is_news", df("is_news").cast("double"))
      .withColumn("lengthyLinkDomain", df("lengthyLinkDomain").cast("double"))
      .withColumn("linkwordscore", df("linkwordscore").cast("double"))
      .withColumn("news_front_page", df("news_front_page").cast("double"))
      .withColumn("non_markup_alphanum_characters", df("non_markup_alphanum_characters").cast("double"))
      .withColumn("numberOfLinks", df("numberOfLinks").cast("double"))
      .withColumn("numwords_in_url", df("numwords_in_url").cast("double"))
      .withColumn("parametrizedLinkRatio", df("parametrizedLinkRatio").cast("double"))
      .withColumn("spelling_errors_ratio", df("spelling_errors_ratio").cast("double"))
      .withColumn("label", df("label").cast("double"))
    df1.printSchema()

    // 用户定义的清理函数 ？  user defined function for cleanup of ?
    val replacefunc = udf {(x:Double) => if(x == "?") 0.0 else x}

    val df2 = df1.withColumn("avglinksize", replacefunc(df1("avglinksize")))
      .withColumn("commonlinkratio_1", replacefunc(df1("commonlinkratio_1")))
      .withColumn("commonlinkratio_2", replacefunc(df1("commonlinkratio_2")))
      .withColumn("commonlinkratio_3", replacefunc(df1("commonlinkratio_3")))
      .withColumn("commonlinkratio_4", replacefunc(df1("commonlinkratio_4")))
      .withColumn("compression_ratio", replacefunc(df1("compression_ratio")))
      .withColumn("embed_ratio", replacefunc(df1("embed_ratio")))
      .withColumn("framebased", replacefunc(df1("framebased")))
      .withColumn("frameTagRatio", replacefunc(df1("frameTagRatio")))
      .withColumn("hasDomainLink", replacefunc(df1("hasDomainLink")))
      .withColumn("html_ratio", replacefunc(df1("html_ratio")))
      .withColumn("image_ratio", replacefunc(df1("image_ratio")))
      .withColumn("is_news", replacefunc(df1("is_news")))
      .withColumn("lengthyLinkDomain", replacefunc(df1("lengthyLinkDomain")))
      .withColumn("linkwordscore", replacefunc(df1("linkwordscore")))
      .withColumn("news_front_page", replacefunc(df1("news_front_page")))
      .withColumn("non_markup_alphanum_characters", replacefunc(df1("non_markup_alphanum_characters")))
      .withColumn("numberOfLinks", replacefunc(df1("numberOfLinks")))
      .withColumn("numwords_in_url", replacefunc(df1("numwords_in_url")))
      .withColumn("parametrizedLinkRatio", replacefunc(df1("parametrizedLinkRatio")))
      .withColumn("spelling_errors_ratio", replacefunc(df1("spelling_errors_ratio")))
      .withColumn("label", replacefunc(df1("label")))

    // 删除前 4 列 drop first 4 columns
    val df3 = df2.drop("url").drop("urlid").drop("boilerplate").drop("alchemy_category").drop("alchemy_category_score")

    // 用空值填充 fill null values with
    val df4 = df3.na.fill(0.0)

    //注册表
    df4.registerTempTable("StumbleUpon_PreProc")
    //打印
    df4.printSchema()
    //输出
    println("输出清晰前五列后的数据")
    sqlContext.sql("SELECT * FROM StumbleUpon_PreProc").show()

    //将多列合并为向量列的特征转换器 设置管道  setup pipeline
    val assembler: VectorAssembler = new VectorAssembler()
      .setInputCols(Array("avglinksize", "commonlinkratio_1", "commonlinkratio_2", "commonlinkratio_3", "commonlinkratio_4", "compression_ratio"
        , "embed_ratio", "framebased", "frameTagRatio", "hasDomainLink", "html_ratio", "image_ratio"
        ,"is_news", "lengthyLinkDomain", "linkwordscore", "news_front_page", "non_markup_alphanum_characters", "numberOfLinks"
        ,"numwords_in_url", "parametrizedLinkRatio", "spelling_errors_ratio"))
      .setOutputCol("features")

    val command: String = args(0)

    if(command.equals("NB")) {
      val df5 = prepareForNaiveBayes(df4)

      val nbAssembler: VectorAssembler = new VectorAssembler()
        .setInputCols(Array("avglinksize", "commonlinkratio_1", "commonlinkratio_2", "commonlinkratio_3", "commonlinkratio_4", "compression_ratio"
          , "embed_ratio", "framebased", "frameTagRatio", "hasDomainLink", "html_ratio", "image_ratio"
          ,"is_news", "lengthyLinkDomain", "linkwordscore", "news_front_page", "non_markup_alphanum_characters", "numberOfLinks"
          ,"numwords_in_url", "parametrizedLinkRatio", "spelling_errors_ratio"))
        .setOutputCol("features")

      executeCommand(command, nbAssembler, df5, sc)
    } else
      executeCommand(command, assembler, df4, sc)
  }

  def executeCommand(arg: String, vectorAssembler: VectorAssembler, dataFrame: DataFrame, sparkContext: SparkContext) = arg match {
    case "LR" => LogisticRegressionPipeline.logisticRegressionPipeline(vectorAssembler, dataFrame)

    case "DT" => DecisionTreePipeline.decisionTreePipeline(vectorAssembler, dataFrame)

//    case "RF" => RandomForestPipeline.randomForestPipeline(vectorAssembler, dataFrame)

//    case "GBT" => GradientBoostedTreePipeline.gradientBoostedTreePipeline(vectorAssembler, dataFrame)

    case "NB" => NaiveBayesPipeline.naiveBayesPipeline(vectorAssembler, dataFrame)

//    case "SVM" => SVMPipeline.svmPipeline(sparkContext)
  }

  def prepareForNaiveBayes(dataFrame: DataFrame): DataFrame = {
    // user defined function for cleanup of ?
    val replacefunc = udf {(x:Double) => if(x < 0) 0.0 else x}

    //因为朴素贝叶斯所需参数不能有 负数 所以提前进行转换
    val df5 = dataFrame.withColumn("avglinksize", replacefunc(dataFrame("avglinksize")))
      .withColumn("commonlinkratio_1", replacefunc(dataFrame("commonlinkratio_1")))
      .withColumn("commonlinkratio_2", replacefunc(dataFrame("commonlinkratio_2")))
      .withColumn("commonlinkratio_3", replacefunc(dataFrame("commonlinkratio_3")))
      .withColumn("commonlinkratio_4", replacefunc(dataFrame("commonlinkratio_4")))
      .withColumn("compression_ratio", replacefunc(dataFrame("compression_ratio")))
      .withColumn("embed_ratio", replacefunc(dataFrame("embed_ratio")))
      .withColumn("framebased", replacefunc(dataFrame("framebased")))
      .withColumn("frameTagRatio", replacefunc(dataFrame("frameTagRatio")))
      .withColumn("hasDomainLink", replacefunc(dataFrame("hasDomainLink")))
      .withColumn("html_ratio", replacefunc(dataFrame("html_ratio")))
      .withColumn("image_ratio", replacefunc(dataFrame("image_ratio")))
      .withColumn("is_news", replacefunc(dataFrame("is_news")))
      .withColumn("lengthyLinkDomain", replacefunc(dataFrame("lengthyLinkDomain")))
      .withColumn("linkwordscore", replacefunc(dataFrame("linkwordscore")))
      .withColumn("news_front_page", replacefunc(dataFrame("news_front_page")))
      .withColumn("non_markup_alphanum_characters", replacefunc(dataFrame("non_markup_alphanum_characters")))
      .withColumn("numberOfLinks", replacefunc(dataFrame("numberOfLinks")))
      .withColumn("numwords_in_url", replacefunc(dataFrame("numwords_in_url")))
      .withColumn("parametrizedLinkRatio", replacefunc(dataFrame("parametrizedLinkRatio")))
      .withColumn("spelling_errors_ratio", replacefunc(dataFrame("spelling_errors_ratio")))
      .withColumn("label", replacefunc(dataFrame("label")))

    return df5
  }

  object DFHelper
  def castColumnTo( df: DataFrame, cn: String, tpe: DataType ) : DataFrame = {
    df.withColumn( cn, df(cn).cast(tpe) )
  }
}

