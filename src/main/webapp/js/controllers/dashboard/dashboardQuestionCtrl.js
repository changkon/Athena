(function() {
    var app = angular.module('athena');

    app.controller('DashboardQuestionCtrl', ['$scope','FilePost', 'StoreQuestionPost', '$timeout', function($scope, FilePost, StoreQuestionPost, $timeout) {
        $scope.showQuestions = false;
        $scope.hideRating = true;
        $scope.myFile = { result : null };
        $scope.textModel = { text : null };
        $scope.fileName = "Your PDF File";
        $scope.questions = null;
        $scope.question = {};
        $scope.question.question = "No question currently";
        $scope.answerOnce = false;

        var categories = [
            { topic: 'Cells', subject: 'Biology' },
            { topic: 'Evolution', subject: 'Biology' },
            { topic: 'Anatomy', subject: 'Biology' }
        ];

        // settings
        $scope.settings = {
            category: {
                options: categories,
                selected: categories[0]
            }
        };

        var currentQuestion = -1;

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
            // remove classes
            for (i=0; i< $scope.question.answers.length; i++) {
                var element = angular.element( document.querySelector( '#answer-' + i ) );
                element.removeClass('correct');
                element.removeClass('incorrect');
            }
            $scope.question = $scope.questions.body[questionNumber-1];
            currentQuestion = $scope.question;
            setFontSize($scope.question.questions);
            $scope.hideRating = true;
        };

        $scope.checkAnswer = function(answerNumber) {
            if (!$scope.answerOnce) {
                var correctElement = angular.element( document.querySelector( '#answer-' + $scope.question.answer ) );
                if (answerNumber == $scope.question.answer) {
                    console.log('correct answer');
                    $scope.question.correctAnswer = true;
                } else {
                    console.log('incorrect answer');
                    var incorrectElement = angular.element( document.querySelector( '#answer-' + answerNumber ) );
                    incorrectElement.addClass('incorrect');
                    $scope.question.correctAnswer = false;
                }
                correctElement.addClass('correct');
                $scope.answerOnce = true;
                $scope.hideRating = false;
            }
        };

        $scope.returnMenu = function() {
            $scope.myFile = null;
            $scope.showQuestions = false;
        }

        $scope.uploadFile = function(){
            var file = $scope.myFile.result;
            var category = $scope.settings.category.selected.topic;
            console.log("file received");
            console.log(file);
            var fd = new FormData();

            if (file != null) {
                fd.append('uploadedFile', file);
                fd.append('uploadedCategory', category);
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
                currentQuestion = $scope.question;
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

        $scope.rateQuestion = function(val) {
            console.log(currentQuestion);
            currentQuestion.rating = val;
            console.log("rating question score of " + val);
            $scope.myPromise = StoreQuestionPost.create({}, currentQuestion).$promise.then(function(res){
                console.log(res);
            });
            $scope.hideRating = true;
        }

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
