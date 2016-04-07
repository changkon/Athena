(function() {
    var app = angular.module('athena');

    // custom filter for progressPag
    app.filter('progressRes', function() {
        return function(input, arr, pageNumber, itemsPerPage) {
            var startIndex = (pageNumber - 1)*itemsPerPage;
            return arr.slice(startIndex, startIndex+itemsPerPage);
        }
    });

    app.controller('DashboardQuestionCtrl', ['$scope','FilePost', '$timeout', function($scope, FilePost, $timeout) {
        $scope.showQuestions = false;
        $scope.myFile = { result : null };
        $scope.textModel = { text : null };
        $scope.fileName = "Your PDF File";
        $scope.questions = null;
        $scope.question = {};
        $scope.question.question = "No question currently";
        $scope.answerOnce = false;
        // query pagination settings
        $scope.pag = {
            currentPage: 1,
            maxSize: 10,
            length: 1
        };

        $scope.progressPag = {
            currentPage: 1,
            maxSize: 5,
            itemsPerPage: 5,
            length: 1
        };

        $scope.update = function(questionNumber) {
             $scope.answerOnce = false;
            for (i=0; i< $scope.question.answers.length; i++) {
                var element = angular.element( document.querySelector( '#answer-' + i ) );
                element.removeClass('correct');
                element.removeClass('incorrect');
            }
            var question = $scope.questions.body[questionNumber-1];
            $scope.question.answer = question.answer;
            $scope.question.answers = question.answers;
            $scope.question.questions = question.question.split(/\\n|:\\n/g);
            $scope.question.topic = question.topic;
            setFontSize($scope.question.questions);
        };

//        $scope.progressUpdate = function(pageNumber) {
//            var startIndex = $scope.progressPag.maxSize * pageNumber - $scope.progressPag.maxSize;
//            $scope.progressPag.res = $scope.questions.body.slice(startIndex, startIndex + $scope.progressPag.maxSize);
//        };

        $scope.checkAnswer = function(answerNumber) {
            if (!$scope.answerOnce) {
                var correctElement = angular.element( document.querySelector( '#answer-' + $scope.question.answer ) );
                if (answerNumber == $scope.question.answer) {
                    console.log('correct answer');
                    $scope.question.gotCorrect = true;
                } else {
                    console.log('incorrect answer');
                    var incorrectElement = angular.element( document.querySelector( '#answer-' + answerNumber ) );
                    incorrectElement.addClass('incorrect');
                    $scope.question.gotCorrect = false;
                }
                correctElement.addClass('correct');
                $scope.answerOnce = true;
            }
        };

        $scope.returnMenu = function() {
            $scope.myFile = null;
            $scope.showQuestions = false;
        }

        $scope.uploadFile = function(){
            var file = $scope.myFile.result;
            console.log("file received");
            console.log(file);
            var fd = new FormData();

            if (file != null) {
                fd.append('uploadedFile', file);
            } else {
                text = $scope.textModel.text;
                console.log("text is " + text);
                fd.append('uploadedText', text.replace("’", "'"));
            }
            $scope.myPromise = FilePost.create({}, fd).$promise.then(function(res){
                // update returned object to store gotCorrect
                for (var i = 0; i < res.body.length; i++) {
                    res.body[i].correctAnswer = null;
                    res.body[i].questions = res.body[i].question.split(/\\n|:\\n/g);
                }
                $scope.questions = res;

                $scope.question = $scope.questions.body[0];
//                $scope.question.answer = res.body[0].answer;
//                $scope.question.answers = res.body[0].answers;
//                $scope.question.questions = res.body[0].question.split(/\\n|:\\n/g);
//                $scope.question.topic = res.body[0].topic;
                setFontSize($scope.question.questions);

                // set pagination
                $scope.pag.numfound = res.body.length;
                $scope.pag.currentPage = 1;

                $scope.progressPag.numfound = res.body.length;
                $scope.progressPag.currentPage = 1;

                $scope.showQuestions = true;
            }).catch(function (err) {
                console.log("error: ");
                console.log(err);
            });
        };

        $scope.openFileModal = function() {
            var inputFileElement = document.getElementById('inputFileElement');
            $timeout(function() {
                inputFileElement.click();
            }, 0);
        };

        setFontSize = function(questions) {
            if (questions.length < 2){
                $scope.fontsize = "30px";
                return;
            }
            var max = 0;
            for (var i = 0; i < questions.length; i++) {
                if (questions[i].length > max) {
                    max = questions[i].length;
                }
            }
            console.log("max line length is " + max)
            if (max > 56) {
                $scope.fontsize = "24px";
            }
        };

    }]);
})();
