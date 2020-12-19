import sys.process._
import SwingGeneralFunc.{addSeparator, getGridBox, getOperatorBox}

import java.awt.event.{MouseAdapter, MouseEvent}
import javax.swing.{BoxLayout, JLabel, JPanel, JScrollPane, SwingWorker}
import scala.swing._
import java.awt.Font.ITALIC
import scala.collection.mutable

// Main frame : spell research by criteria
class SearchFrame extends MainFrame {
  // Set Window title
  title = "Spell Searcher"

  // var is mutable contrary to val
  private var nbResult: Int = 0

  private var sparkRequest: Option[SparkRequest.type] = None

  // Initialisation for loading bar and user return message
  private val userInfoLabel: Label = new Label("Waiting for search request")
  userInfoLabel.font = new Font(userInfoLabel.font.getName, ITALIC, userInfoLabel.font.getSize)

  private val progressBar: ProgressBar = new ProgressBar
  private val progressBarGlue: Component = Swing.Glue
  progressBar.visible = false

  // Initialisation for research by Class
  // Round to upper with (if (searcher.getNbClass % nbGridColumn == 0) 0 else 1)
  private val checkBoxClassMap: mutable.TreeMap[String, CheckBox] = new mutable.TreeMap()

  private val btnClassAnd = new RadioButton("AND")
  private val btnClassOr = new RadioButton("OR")
  btnClassAnd.selected = true
  new ButtonGroup(btnClassAnd, btnClassOr)

  // Initialisation for research by School
  private val checkBoxSchoolMap: mutable.TreeMap[String, CheckBox] = new mutable.TreeMap()

  // Initialisation for research by Component
  private var checkBoxComponentMap: mutable.TreeMap[String, CheckBox] = new mutable.TreeMap()
  private val btnComponentAnd = new RadioButton("AND")
  private val btnComponentOr = new RadioButton("OR")
  btnComponentAnd.selected = true
  new ButtonGroup(btnComponentAnd, btnComponentOr)

  // Initialisation for research by Spell Resistance
  private val btnSpellResistanceYes = new RadioButton("Yes")
  private val btnSpellResistanceNo = new RadioButton("No")
  private val btnSpellResistanceNotCheck = new RadioButton("Not checked")
  btnSpellResistanceNotCheck.selected = true
  new ButtonGroup(btnSpellResistanceYes, btnSpellResistanceNo, btnSpellResistanceNotCheck)

  // Initialisation for research by Description
  private val descriptionTextField = new TextField()

  // Initialisation for buttons
  private val resetBtn: Button = Button("Reset") { resetField() }
  private val researchBtn: Button = Button("Search") { launchResearch() }
  private val relaunchScrapyBtn: Button = Button("Update Database") { updateDatabase() }

  // https://flatuicolors.com/palette/defo
  resetBtn.background = new Color(231, 76, 60)
  researchBtn.background = new Color(46, 204, 113)
  relaunchScrapyBtn.background = new Color(230, 126, 34)

  // Initialisation for results
  private val jPanelResult = new JPanel()
  jPanelResult.setLayout(new BoxLayout(jPanelResult, BoxLayout.Y_AXIS))
  private val labelNbResult = new Label(s"Number: $nbResult")

  initInterface()

  /*
   *  PUBLIC FUNCTIONS
   */
  def setSparkRequest(sparkRequest: SparkRequest.type): Unit = {
    this.sparkRequest = Some(sparkRequest)
  }

  def disableResearch(msg: String): Unit = {
    researchBtn.enabled = false
    relaunchScrapyBtn.enabled = false

    progressBar.visible = true
    progressBarGlue.visible = false

    progressBar.indeterminate = true

    userInfoLabel.text = msg
  }

  def enableResearch(msg: String): Unit = {
    researchBtn.enabled = true
    relaunchScrapyBtn.enabled = true

    userInfoLabel.text = msg

    progressBarGlue.visible = true
    progressBar.visible = false
  }

  /*
   *  PRIVATE FUNCTIONS
   */
  private def initInterface(): Unit = {
    contents = new BoxPanel(Orientation.Vertical) {
      // Set frame border margin
      border = Swing.EmptyBorder(10, 10, 10, 10)

      // Add box for loading bar and message
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += userInfoLabel

        contents += Swing.HStrut(20)

        contents += progressBarGlue
        contents += progressBar

        contents += Swing.HStrut(50)

        contents += relaunchScrapyBtn
      }

      contents += Swing.VStrut(10)
      contents += new Separator()
      contents += new Separator()
      contents += Swing.VStrut(10)

      contents += new BoxPanel(Orientation.Horizontal) {
        contents += new Label("Choose any of the options below to narrow the results of your research")

        // Takes remaining space
        contents += Swing.Glue
      }

      addSeparator(contents)

      // Add box for class selection
      contents += getGridBox("Class", checkBoxClassMap, Searcher.getAllClassName)

      // Add box for class operator selection (AND or OR)
      contents += getOperatorBox(btnClassAnd, btnClassOr)

      addSeparator(contents)

      // Add box for school selection
      contents += getGridBox("School", checkBoxSchoolMap, Searcher.getAllSchoolName)

      // Add box description for school selection
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += new Label("OR operator is applied, because a spell can only have one school. You will get the spells for each selected school")
        contents += Swing.Glue
      }

      addSeparator(contents)

      // Add box for component selection
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += new Label("Component:")
        contents += Swing.HStrut(10)

        // Add available components dynamically
        for (componentName <- Searcher.getAllComponentName) {
          checkBoxComponentMap += (componentName -> new CheckBox(componentName))
          contents += checkBoxComponentMap(componentName)

          // Add horizontal spacing
          contents += Swing.HStrut(5)
        }
        // Takes remaining space
        contents += Swing.Glue
      }

      // Add box for component operator selection (AND or OR)
      contents += getOperatorBox(btnComponentAnd, btnComponentOr)

      addSeparator(contents)

      // Add box for spell resistance selection
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += new Label("Spell Resistance:")
        contents += Swing.HStrut(10)

        contents += btnSpellResistanceYes
        contents += btnSpellResistanceNo
        contents += btnSpellResistanceNotCheck

        contents += Swing.Glue
      }

      addSeparator(contents)

      // Add box for research by spell description (full text search)
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += new Label("Description:")
        contents += Swing.HStrut(10)

        contents += descriptionTextField
        contents += Swing.Glue
      }

      contents += Swing.VStrut(10)
      contents += Swing.Glue

      // Add reset and research buttons
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += resetBtn
        contents += Swing.HStrut(50)
        contents += researchBtn
      }

      addSeparator(contents)
      // Add box for spell result number
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += new Label("Spell results")
        contents += Swing.Glue
        contents += labelNbResult
      }

      contents += Swing.VStrut(5)

      private val jScrollPaneResult = new JScrollPane(jPanelResult)
      jScrollPaneResult.setPreferredSize(new Dimension(0, 180))
      // https://stackoverflow.com/questions/5583495/how-do-i-speed-up-the-scroll-speed-in-a-jscrollpane-when-using-the-mouse-wheel
      jScrollPaneResult.getVerticalScrollBar.setUnitIncrement(16)

      // Use Component.wrap() for link between javax.swing and scala.swing
      // Add scroll pane for spell results
      contents += Component.wrap(jScrollPaneResult)
    }
  }

  private def launchResearch(): Unit = {
    disableResearch("Processing request")
    // Reset result field to only display new results
    resetResult()

    val classArray: List[String] = getArrayFromCheckbox(checkBoxClassMap)
    val schoolArray: List[String] = getArrayFromCheckbox(checkBoxSchoolMap)
    val componentArray: List[String] = getArrayFromCheckbox(checkBoxComponentMap)

    val classOperator: String = getOperatorFromButton(btnClassOr)
    val componentOperator: String = getOperatorFromButton(btnComponentOr)
    val spellResistance: String = getSpellResistance

    val description: List[String] = getDescription

    // SwingWorker to perform long process in background thread in order not to freeze the UI
    // https://www.geeksforgeeks.org/swingworker-in-java/
    // https://docs.oracle.com/javase/6/docs/api/javax/swing/SwingWorker.html
    val worker = new SwingWorker[List[String], List[String]] {
      override protected def doInBackground(): List[String] = {
        val spellInfo: List[String] = sparkRequest.get.getSpellList(classArray, classOperator, schoolArray, componentArray, componentOperator, spellResistance, description)

        spellInfo
      }

      override protected def done(): Unit = {
        val spellInfo: List[String] = get()

        for (spellName <- spellInfo){
          addSpell(spellName)
        }
        enableResearch("Request successful! Waiting for search request")
      }
    }

    worker.execute()
  }

  private def addSpell(spellName: String): Unit = {
    val jLabel = new JLabel(spellName)

    jLabel.addMouseListener(new MouseAdapter() {
      // https://stackoverflow.com/questions/2440134/is-this-the-proper-way-to-initialize-null-references-in-scala
      // https://alvinalexander.com/scala/initialize-scala-variables-option-none-some-null-idiom/
      private var spellDisplay = Option.empty[SpellFrame]

      override def mouseClicked(e: MouseEvent) {
        // Create the SpellFrame if it has not been created before
        if (spellDisplay.isEmpty){
          disableResearch(s"Looking for spell: '$spellName''")

          val worker = new SwingWorker[(Map[String, String], List[String]), (Map[String, String], List[String])] {
            override protected def doInBackground(): (Map[String, String], List[String]) = {
              // Retrieve spell and creature info
              val spellInfo: Map[String, String] = sparkRequest.get.getSpellInfo(spellName)
              val creatureList: List[String] = sparkRequest.get.getCreatureList(spellName)
              (spellInfo, creatureList)
            }

            override protected def done(): Unit = {
              val spellCreatureInfo: (Map[String, String], List[String]) = get()
              val spellInfo: Map[String, String] = spellCreatureInfo._1
              val creatureInfo: List[String] = spellCreatureInfo._2
              // Display new frame with spell and creature info
              spellDisplay = Some(new SpellFrame(sparkRequest.get, spellName, spellInfo, creatureInfo))
              spellDisplay.get.centerOnScreen()
              spellDisplay.get.open()

              enableResearch("Spell info retrieved! Waiting for search request")
            }
          }
          worker.execute()
        }
        // elsewhere simply bring the window to the front
        else{
          spellDisplay.get.open()
        }
      }
    })
    jPanelResult.add(jLabel).revalidate()

    nbResult += 1
    labelNbResult.text = s"Number: $nbResult"
  }

  private def getArrayFromCheckbox(map: mutable.TreeMap[String, CheckBox]): List[String] = {
    var stringArray: List[String] = List[String]()

    // Get selected checkbox, and put key in Array
    for (mapElement <- map) {
      if (mapElement._2.selected) {
        stringArray :+= mapElement._1
      }
    }

    stringArray
  }

  private def getOperatorFromButton(button: RadioButton): String = {
    // Get selected operator : set operator to OR if selected, else set to AND
    if (button.selected) {
      "OR"
    }
    else {
      "AND"
    }
  }

  private def getSpellResistance: String = {
    if (btnSpellResistanceYes.selected) {
      true.toString
    }
    else if (btnSpellResistanceNo.selected) {
      false.toString
    }
    else {
      ""
    }
  }

  private def getDescription: List[String] = {
    val tempDesc = descriptionTextField.text.replaceAll("[^a-zA-Z]", " ").toLowerCase

    if (tempDesc.isEmpty){
      List[String]()
    }
    else{
      tempDesc.split(" +").toList
    }
  }

  private def resetField(): Unit = {
    userInfoLabel.text = "Waiting for search request"
    // Reset classes buttons and set AND operator
    for (classElement <- checkBoxClassMap) { classElement._2.selected = false }
    btnClassAnd.selected = true

    // Reset schools buttons and set AND operator
    for (schoolElement <- checkBoxSchoolMap) { schoolElement._2.selected = false }

    // Reset components buttons and set AND operator
    for (componentElement <- checkBoxComponentMap) { componentElement._2.selected = false }
    btnComponentAnd.selected = true

    // Set spell resistance not checked
    btnSpellResistanceNotCheck.selected = true

    // Reset description text fields
    descriptionTextField.text = ""

    resetResult()
  }

  private def resetResult(): Unit = {
    // Remove all spells result
    jPanelResult.removeAll()
    jPanelResult.revalidate()
    jPanelResult.repaint()

    // Reset result number
    nbResult = 0
    labelNbResult.text = s"Number: $nbResult"
  }

  private def updateDatabase(): Unit = {
    disableResearch("Updating Database")

    // SwingWorker to perform long process in background thread in order not to freeze the UI
    // https://www.geeksforgeeks.org/swingworker-in-java/
    // https://docs.oracle.com/javase/6/docs/api/javax/swing/SwingWorker.html
    val worker = new SwingWorker[Int, Int] {
      override protected def doInBackground(): Int = {
        //println("Loading spell and creature data from https://www.aonprd.com/")
        // Execute python crawler
        // External command: https://alvinalexander.com/scala/scala-execute-exec-external-system-commands-in-scala/
        val crawler: Int = "python3 src/main/python/main.py".!
        crawler
      }

      override protected def done(): Unit = {
        val crawler: Int = get()
        if (crawler == 0){
          enableResearch("Update successful! Waiting for search request")
        }
        else {
          enableResearch(s"Failed update. Crawler exit code: $crawler")
          //println(s"There was an error with the python crawler.\nExit code: $crawler")
        }
      }
    }

    worker.execute()
  }
}