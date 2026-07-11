import React from 'react';
import { motion } from 'framer-motion';
import { Shield, Camera, Lock, Clock, AlertTriangle, CheckCircle, X, Maximize } from 'lucide-react';
import { Button } from '@/components/common/Button';
import { Badge } from '@/components/common/Badge';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants';
import { mockExams } from '@/services/mockData';
import { formatSeconds } from '@/utils';
import { useState, useEffect } from 'react';

export function SecureBrowserPage() {
  const navigate = useNavigate();
  const exam = mockExams[0];
  const [timeLeft, setTimeLeft] = useState(exam.duration * 60);
  const [currentQ, setCurrentQ] = useState(0);
  const [answers, setAnswers] = useState<Record<number, string>>({});
  const [submitOpen, setSubmitOpen] = useState(false);

  useEffect(() => {
    const timer = setInterval(() => setTimeLeft((t) => Math.max(0, t - 1)), 1000);
    return () => clearInterval(timer);
  }, []);

  const questions = [
    { id: 0, text: 'Which data structure uses LIFO (Last In First Out) ordering?', options: ['Queue', 'Stack', 'Linked List', 'Binary Tree'], marks: 5 },
    { id: 1, text: 'What is the time complexity of binary search?', options: ['O(n)', 'O(n²)', 'O(log n)', 'O(n log n)'], marks: 5 },
    { id: 2, text: 'Which sorting algorithm has the best average case complexity?', options: ['Bubble Sort', 'Quick Sort', 'Merge Sort', 'Insertion Sort'], marks: 5 },
    { id: 3, text: 'What does SQL stand for?', options: ['Structured Query Language', 'Simple Query Language', 'Structured Question Language', 'System Query Language'], marks: 5 },
    { id: 4, text: 'Which tree traversal visits root first, then left, then right?', options: ['In-order', 'Post-order', 'Pre-order', 'Level-order'], marks: 5 },
  ];

  const answered = Object.keys(answers).length;
  const progress = (answered / questions.length) * 100;
  const timeWarning = timeLeft < 600;

  return (
    <div className="fixed inset-0 bg-dark-950 text-white flex flex-col overflow-hidden z-50">
      {/* Top bar */}
      <div className="flex items-center justify-between px-6 py-3 border-b border-dark-800 bg-dark-900 flex-shrink-0">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <div className="h-7 w-7 rounded-lg bg-gradient-primary flex items-center justify-center text-white font-bold text-sm">S</div>
            <span className="font-display font-semibold text-white">SecureIQ</span>
          </div>
          <div className="h-4 w-px bg-dark-700" />
          <div>
            <p className="text-sm font-medium text-white">{exam.title}</p>
            <p className="text-xs text-dark-400">{exam.code}</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          {/* Camera active */}
          <div className="flex items-center gap-1.5 text-xs text-green-400">
            <Camera className="h-3.5 w-3.5" />
            <span>Camera Active</span>
          </div>
          <div className="h-4 w-px bg-dark-700" />

          {/* Timer */}
          <div className={`flex items-center gap-2 px-3 py-1.5 rounded-lg ${timeWarning ? 'bg-danger/20 text-danger-400' : 'bg-dark-800 text-white'}`}>
            <Clock className={`h-3.5 w-3.5 ${timeWarning ? 'animate-pulse' : ''}`} />
            <span className="font-mono font-bold text-base">{formatSeconds(timeLeft)}</span>
          </div>
          <div className="h-4 w-px bg-dark-700" />

          {/* Secure mode */}
          <div className="flex items-center gap-1.5 text-xs text-blue-400">
            <Lock className="h-3.5 w-3.5" />
            <span>Secure Mode</span>
          </div>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* Question panel */}
        <div className="w-64 border-r border-dark-800 bg-dark-900 flex flex-col flex-shrink-0">
          <div className="p-4 border-b border-dark-800">
            <p className="text-xs font-semibold text-dark-400 uppercase tracking-wide mb-2">Progress</p>
            <div className="flex justify-between text-xs text-dark-300 mb-1.5">
              <span>{answered} answered</span>
              <span>{questions.length - answered} remaining</span>
            </div>
            <div className="h-2 w-full rounded-full bg-dark-700 overflow-hidden">
              <div className="h-full rounded-full bg-primary transition-all duration-500" style={{ width: `${progress}%` }} />
            </div>
          </div>
          <div className="flex-1 overflow-y-auto p-3">
            <div className="grid grid-cols-4 gap-1.5">
              {questions.map((q) => (
                <button
                  key={q.id}
                  onClick={() => setCurrentQ(q.id)}
                  className={`h-8 w-full rounded-md text-xs font-semibold transition-colors ${
                    answers[q.id]
                      ? 'bg-success/20 text-green-400 border border-success/30'
                      : currentQ === q.id
                      ? 'bg-primary text-white'
                      : 'bg-dark-800 text-dark-400 hover:bg-dark-700 border border-dark-700'
                  }`}
                >
                  {q.id + 1}
                </button>
              ))}
            </div>
          </div>
          <div className="p-3 border-t border-dark-800">
            <Button
              variant="primary"
              className="w-full"
              onClick={() => setSubmitOpen(true)}
            >
              Submit Exam
            </Button>
          </div>
        </div>

        {/* Question content */}
        <div className="flex-1 overflow-y-auto p-8">
          <motion.div
            key={currentQ}
            initial={{ opacity: 0, x: 16 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.25 }}
            className="max-w-2xl mx-auto"
          >
            <div className="flex items-center gap-3 mb-6">
              <span className="h-8 w-8 rounded-full bg-primary/20 text-primary text-sm font-bold flex items-center justify-center">
                {currentQ + 1}
              </span>
              <span className="text-xs text-dark-400">{questions[currentQ].marks} marks</span>
            </div>

            <p className="text-white text-lg font-medium mb-6 leading-relaxed">
              {questions[currentQ].text}
            </p>

            <div className="space-y-3">
              {questions[currentQ].options.map((opt, i) => {
                const letter = String.fromCharCode(65 + i);
                const isSelected = answers[currentQ] === opt;
                return (
                  <motion.button
                    key={opt}
                    whileTap={{ scale: 0.99 }}
                    onClick={() => setAnswers((prev) => ({ ...prev, [currentQ]: opt }))}
                    className={`w-full flex items-center gap-4 p-4 rounded-xl border text-left transition-all duration-200 ${
                      isSelected
                        ? 'border-primary bg-primary/10 text-primary'
                        : 'border-dark-700 bg-dark-800 text-dark-200 hover:border-dark-500'
                    }`}
                  >
                    <span className={`h-8 w-8 rounded-full border-2 flex items-center justify-center text-sm font-bold flex-shrink-0 ${
                      isSelected ? 'border-primary bg-primary text-white' : 'border-dark-600 text-dark-400'
                    }`}>
                      {letter}
                    </span>
                    <span className="text-sm">{opt}</span>
                    {isSelected && <CheckCircle className="h-4 w-4 text-primary ml-auto" />}
                  </motion.button>
                );
              })}
            </div>

            {/* Navigation */}
            <div className="flex justify-between mt-8">
              <Button
                variant="outline"
                className="border-dark-700 text-dark-300 hover:bg-dark-800"
                onClick={() => setCurrentQ((q) => Math.max(0, q - 1))}
                disabled={currentQ === 0}
              >
                Previous
              </Button>
              {currentQ < questions.length - 1 ? (
                <Button variant="primary" onClick={() => setCurrentQ((q) => q + 1)}>
                  Next
                </Button>
              ) : (
                <Button variant="success" onClick={() => setSubmitOpen(true)}>
                  Review & Submit
                </Button>
              )}
            </div>
          </motion.div>
        </div>

        {/* Camera feed */}
        <div className="w-52 border-l border-dark-800 bg-dark-900 flex-shrink-0 flex flex-col">
          <div className="p-3 border-b border-dark-800">
            <p className="text-xs font-semibold text-dark-400 uppercase tracking-wide">AI Monitor</p>
          </div>
          <div className="p-3 space-y-3 flex-1">
            {/* Camera */}
            <div className="aspect-video rounded-lg bg-dark-950 flex items-center justify-center border border-dark-700 relative">
              <div className="text-center">
                <Camera className="h-6 w-6 text-dark-600 mx-auto" />
                <p className="text-[10px] text-dark-600 mt-1">Live Feed</p>
              </div>
              <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-green-400 animate-pulse" />
            </div>

            {/* Status */}
            <div className="space-y-1.5">
              {[
                { label: 'Face Detected', ok: true },
                { label: 'Single Person', ok: true },
                { label: 'Focus Active', ok: true },
              ].map((item) => (
                <div key={item.label} className="flex items-center gap-2 text-xs">
                  <CheckCircle className="h-3 w-3 text-green-400 flex-shrink-0" />
                  <span className="text-dark-400">{item.label}</span>
                </div>
              ))}
            </div>

            {/* Risk */}
            <div className="p-2.5 rounded-lg bg-dark-800">
              <div className="flex justify-between text-[10px] mb-1.5">
                <span className="text-dark-400">Risk Score</span>
                <span className="text-green-400 font-bold">Low</span>
              </div>
              <div className="h-1.5 rounded-full bg-dark-700 overflow-hidden">
                <div className="h-full w-[8%] rounded-full bg-green-400" />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Submit confirm overlay */}
      {submitOpen && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-dark-900 border border-dark-700 rounded-2xl p-6 max-w-sm w-full mx-4"
          >
            <h3 className="font-display font-bold text-white text-lg mb-2">Submit Exam?</h3>
            <p className="text-dark-400 text-sm mb-4">
              You have answered {answered} of {questions.length} questions. Are you sure you want to submit?
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setSubmitOpen(false)}
                className="flex-1 py-2.5 rounded-lg border border-dark-600 text-dark-300 text-sm font-medium hover:bg-dark-800 transition-colors"
              >
                Continue Exam
              </button>
              <Button
                variant="primary"
                className="flex-1"
                onClick={() => navigate(ROUTES.STUDENT.RESULTS)}
              >
                Submit
              </Button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
