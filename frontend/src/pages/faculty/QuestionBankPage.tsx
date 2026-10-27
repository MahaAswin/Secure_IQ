import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ClipboardList, Plus, Trash2, Edit, Save, X, HelpCircle,
  FileText, CheckCircle2, ChevronRight, BookOpen, AlertCircle
} from 'lucide-react';
import { Input, Textarea, Select } from '@/components/forms/FormFields';
import { Button } from '@/components/common/Button';
import { Card, CardHeader } from '@/components/common/Card';
import { Badge } from '@/components/common/Badge';
import { ConfirmDialog } from '@/components/dialogs/Dialog';
import api from '@/services/api';
import toast from 'react-hot-toast';

export function QuestionBankPage() {
  const [banks, setBanks] = useState<any[]>([]);
  const [selectedBank, setSelectedBank] = useState<any>(null);
  const [questions, setQuestions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingQuestions, setLoadingQuestions] = useState(false);

  // Bank Form State
  const [bankModalOpen, setBankModalOpen] = useState(false);
  const [bankFormData, setBankFormData] = useState({ bankName: '', description: '', subjectId: '', departmentId: '' });
  const [subjects, setSubjects] = useState<any[]>([]);

  // Question Form State
  const [questionModalOpen, setQuestionModalOpen] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState<any>(null);
  const [questionFormData, setQuestionFormData] = useState({
    questionText: '',
    questionType: 'MCQ',
    difficulty: 'EASY',
    marks: '5',
    options: ['', '', '', ''],
    correctAnswer: '',
    explanation: ''
  });

  const [confirmDeleteBankOpen, setConfirmDeleteBankOpen] = useState(false);
  const [confirmDeleteQuestionOpen, setConfirmDeleteQuestionOpen] = useState(false);
  const [deletingQuestionId, setDeletingQuestionId] = useState<number | null>(null);

  useEffect(() => {
    async function loadInitialData() {
      try {
        const [banksRes, profileRes] = await Promise.all([
          api.get('/v1/question-banks', { params: { size: 100 } }),
          api.get('/v1/faculties/profile')
        ]);
        const bankList = banksRes.data.data.content || [];
        setBanks(bankList);
        if (bankList.length > 0) {
          setSelectedBank(bankList[0]);
        }
        setSubjects(profileRes.data.data.subjects || []);
        if (profileRes.data.data) {
          setBankFormData((prev) => ({
            ...prev,
            departmentId: profileRes.data.data.department?.id?.toString() || ''
          }));
        }
      } catch (err) {
        toast.error('Failed to load question banks');
      } finally {
        setLoading(false);
      }
    }
    loadInitialData();
  }, []);

  useEffect(() => {
    async function loadQuestions() {
      if (!selectedBank) {
        setQuestions([]);
        return;
      }
      setLoadingQuestions(true);
      try {
        const res = await api.get('/v1/questions', {
          params: { bankId: selectedBank.id, size: 100 }
        });
        setQuestions(res.data.data.content || []);
      } catch (err) {
        toast.error('Failed to load questions for selected bank');
      } finally {
        setLoadingQuestions(false);
      }
    }
    loadQuestions();
  }, [selectedBank]);

  const handleCreateBank = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bankFormData.bankName || !bankFormData.subjectId) {
      toast.error('Please fill in required fields');
      return;
    }
    try {
      const payload = {
        bankName: bankFormData.bankName,
        description: bankFormData.description,
        subjectId: parseInt(bankFormData.subjectId),
        departmentId: parseInt(bankFormData.departmentId)
      };
      const res = await api.post('/v1/question-banks', payload);
      const newBank = res.data.data;
      setBanks((prev) => [...prev, newBank]);
      setSelectedBank(newBank);
      setBankModalOpen(false);
      setBankFormData({ bankName: '', description: '', subjectId: '', departmentId: bankFormData.departmentId });
      toast.success('Question bank created successfully!');
    } catch (err) {
      toast.error('Failed to create question bank');
    }
  };

  const handleDeleteBank = async () => {
    if (!selectedBank) return;
    try {
      await api.delete(`/v1/question-banks/${selectedBank.id}`);
      const remaining = banks.filter((b) => b.id !== selectedBank.id);
      setBanks(remaining);
      setSelectedBank(remaining.length > 0 ? remaining[0] : null);
      setConfirmDeleteBankOpen(false);
      toast.success('Question bank deleted successfully');
    } catch (err) {
      toast.error('Failed to delete question bank');
    }
  };

  const handleOpenQuestionModal = (q: any = null) => {
    if (q) {
      setEditingQuestion(q);
      setQuestionFormData({
        questionText: q.questionText,
        questionType: q.questionType,
        difficulty: q.difficulty,
        marks: q.marks?.toString() || '5',
        options: q.options || ['', '', '', ''],
        correctAnswer: q.correctAnswer,
        explanation: q.explanation || ''
      });
    } else {
      setEditingQuestion(null);
      setQuestionFormData({
        questionText: '',
        questionType: 'MCQ',
        difficulty: 'EASY',
        marks: '5',
        options: ['', '', '', ''],
        correctAnswer: '',
        explanation: ''
      });
    }
    setQuestionModalOpen(true);
  };

  const handleSaveQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!questionFormData.questionText || !questionFormData.correctAnswer) {
      toast.error('Please enter question text and correct answer');
      return;
    }
    try {
      const payload = {
        questionText: questionFormData.questionText,
        questionType: questionFormData.questionType,
        difficulty: questionFormData.difficulty,
        marks: parseInt(questionFormData.marks),
        options: questionFormData.options.filter((o) => o.trim() !== ''),
        correctAnswer: questionFormData.correctAnswer,
        explanation: questionFormData.explanation,
        bankId: selectedBank.id
      };

      if (editingQuestion) {
        const res = await api.put(`/v1/questions/${editingQuestion.id}`, payload);
        setQuestions((prev) => prev.map((q) => (q.id === editingQuestion.id ? res.data.data : q)));
        toast.success('Question updated successfully!');
      } else {
        const res = await api.post('/v1/questions', payload);
        setQuestions((prev) => [...prev, res.data.data]);
        toast.success('Question added successfully!');
      }
      setQuestionModalOpen(false);
    } catch (err) {
      toast.error('Failed to save question');
    }
  };

  const handleDeleteQuestion = async () => {
    if (deletingQuestionId === null) return;
    try {
      await api.delete(`/v1/questions/${deletingQuestionId}`);
      setQuestions((prev) => prev.filter((q) => q.id !== deletingQuestionId));
      setConfirmDeleteQuestionOpen(false);
      setDeletingQuestionId(null);
      toast.success('Question deleted successfully');
    } catch (err) {
      toast.error('Failed to delete question');
    }
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-display font-bold text-foreground text-2xl">Question Bank</h1>
          <p className="text-muted-foreground text-sm mt-1">
            Manage subject-specific question repositories and questions
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            leftIcon={<Plus className="h-4 w-4" />}
            onClick={() => setBankModalOpen(true)}
          >
            New Bank
          </Button>
          {selectedBank && (
            <Button
              variant="primary"
              leftIcon={<Plus className="h-4 w-4" />}
              onClick={() => handleOpenQuestionModal()}
            >
              Add Question
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Left Side: Banks List */}
        <div className="space-y-3 lg:col-span-1">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Repositories</p>
          <div className="space-y-2">
            {banks.length === 0 ? (
              <p className="text-xs text-muted-foreground py-4 text-center">No question banks found.</p>
            ) : (
              banks.map((b) => (
                <button
                  key={b.id}
                  onClick={() => setSelectedBank(b)}
                  className={`w-full text-left p-3.5 rounded-xl border transition-all duration-200 ${
                    selectedBank?.id === b.id
                      ? 'border-primary bg-primary/5 text-primary shadow-glow-sm font-semibold'
                      : 'border-border bg-card hover:border-primary/30 text-foreground'
                  }`}
                >
                  <div className="flex items-start gap-2.5">
                    <ClipboardList className="h-4 w-4 flex-shrink-0 mt-0.5" />
                    <div className="min-w-0">
                      <p className="text-sm truncate">{b.bankName}</p>
                      <p className="text-xs text-muted-foreground mt-0.5 truncate">{b.description || 'No description'}</p>
                    </div>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>

        {/* Right Side: Selected Bank Details & Questions */}
        <div className="lg:col-span-3 space-y-4">
          {selectedBank ? (
            <>
              {/* Selected Bank Summary Card */}
              <Card>
                <div className="flex justify-between items-start gap-4">
                  <div>
                    <h2 className="font-display font-semibold text-lg text-foreground">{selectedBank.bankName}</h2>
                    <p className="text-sm text-muted-foreground mt-1">{selectedBank.description}</p>
                    <div className="flex gap-2 mt-3 flex-wrap">
                      <Badge variant="primary">{selectedBank.subject?.subjectCode || 'Subject'}</Badge>
                      <Badge variant="default">{selectedBank.department?.name || 'Department'}</Badge>
                      <Badge variant="success">{questions.length} Questions</Badge>
                    </div>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-danger hover:bg-danger/10"
                    leftIcon={<Trash2 className="h-4 w-4" />}
                    onClick={() => setConfirmDeleteBankOpen(true)}
                  >
                    Delete Bank
                  </Button>
                </div>
              </Card>

              {/* Questions List */}
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <h3 className="font-semibold text-foreground text-base">Questions ({questions.length})</h3>
                </div>

                {loadingQuestions ? (
                  <div className="flex justify-center py-12">
                    <div className="h-6 w-6 rounded-full border-2 border-primary border-t-transparent animate-spin" />
                  </div>
                ) : questions.length === 0 ? (
                  <Card className="p-12 text-center">
                    <HelpCircle className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                    <p className="text-sm text-muted-foreground">No questions in this repository yet.</p>
                    <Button
                      variant="outline"
                      size="sm"
                      className="mt-4"
                      leftIcon={<Plus className="h-4 w-4" />}
                      onClick={() => handleOpenQuestionModal()}
                    >
                      Add First Question
                    </Button>
                  </Card>
                ) : (
                  <div className="space-y-3">
                    {questions.map((q, i) => (
                      <motion.div
                        key={q.id}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: i * 0.05 }}
                        className="p-5 rounded-xl border border-border bg-card space-y-4"
                      >
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex items-start gap-3">
                            <span className="h-6 w-6 rounded-full bg-primary/20 text-primary text-xs font-bold flex items-center justify-center flex-shrink-0 mt-0.5">
                              {i + 1}
                            </span>
                            <div>
                              <p className="font-medium text-foreground leading-relaxed">{q.questionText}</p>
                              <div className="flex gap-2 mt-2">
                                <Badge variant="default" className="text-[10px]">{q.questionType}</Badge>
                                <Badge
                                  variant={
                                    q.difficulty === 'HARD' ? 'danger' : q.difficulty === 'MEDIUM' ? 'warning' : 'success'
                                  }
                                  className="text-[10px]"
                                >
                                  {q.difficulty}
                                </Badge>
                                <Badge variant="primary" className="text-[10px]">{q.marks} Marks</Badge>
                              </div>
                            </div>
                          </div>
                          <div className="flex gap-1.5 flex-shrink-0">
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              onClick={() => handleOpenQuestionModal(q)}
                            >
                              <Edit className="h-3.5 w-3.5 text-muted-foreground" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              onClick={() => {
                                setDeletingQuestionId(q.id);
                                setConfirmDeleteQuestionOpen(true);
                              }}
                            >
                              <Trash2 className="h-3.5 w-3.5 text-danger" />
                            </Button>
                          </div>
                        </div>

                        {/* Options */}
                        {q.options && q.options.length > 0 && (
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5 pl-9">
                            {q.options.map((opt: string, idx: number) => (
                              <div
                                key={idx}
                                className={`flex items-center gap-2 p-2.5 rounded-lg border text-sm ${
                                  opt === q.correctAnswer
                                    ? 'border-success/30 bg-success/5 text-success'
                                    : 'border-border bg-muted/20 text-muted-foreground'
                                }`}
                              >
                                <span className="font-semibold text-xs">{String.fromCharCode(65 + idx)}.</span>
                                <span>{opt}</span>
                                {opt === q.correctAnswer && <CheckCircle2 className="h-4 w-4 ml-auto flex-shrink-0" />}
                              </div>
                            ))}
                          </div>
                        )}

                        {q.explanation && (
                          <div className="pl-9 py-2 border-l-2 border-border text-xs text-muted-foreground leading-relaxed">
                            <span className="font-semibold text-foreground">Explanation: </span>
                            {q.explanation}
                          </div>
                        )}
                      </motion.div>
                    ))}
                  </div>
                )}
              </div>
            </>
          ) : (
            <Card className="p-12 text-center">
              <ClipboardList className="h-12 w-12 text-muted-foreground mx-auto mb-3" />
              <h3 className="font-semibold text-foreground text-lg mb-1">Welcome to Question Bank</h3>
              <p className="text-sm text-muted-foreground max-w-sm mx-auto mb-6">
                Create a question repository to begin structuring your test banks and exams.
              </p>
              <Button
                variant="primary"
                leftIcon={<Plus className="h-4 w-4" />}
                onClick={() => setBankModalOpen(true)}
              >
                Create Question Bank
              </Button>
            </Card>
          )}
        </div>
      </div>

      {/* Bank Creation Modal */}
      {bankModalOpen && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-card border border-border rounded-2xl p-6 max-w-md w-full mx-4 shadow-soft"
          >
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-display font-bold text-foreground text-lg">New Question Bank</h3>
              <button onClick={() => setBankModalOpen(false)} className="text-muted-foreground hover:text-foreground">
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleCreateBank} className="space-y-4">
              <Input
                label="Bank Name *"
                placeholder="e.g. Data Structures Midterm Bank"
                value={bankFormData.bankName}
                onChange={(e) => setBankFormData({ ...bankFormData, bankName: e.target.value })}
                required
              />
              <Textarea
                label="Description"
                placeholder="e.g. Question repository containing arrays, lists and tree questions."
                value={bankFormData.description}
                onChange={(e) => setBankFormData({ ...bankFormData, description: e.target.value })}
              />
              <Select
                label="Subject *"
                value={bankFormData.subjectId}
                onChange={(e) => setBankFormData({ ...bankFormData, subjectId: e.target.value })}
                required
                placeholder="Select subject"
                options={subjects.map((sub) => ({
                  value: sub.id.toString(),
                  label: `${sub.subjectCode} - ${sub.subjectName}`
                }))}
              />
              <div className="flex gap-3 pt-2">
                <Button
                  variant="outline"
                  className="flex-1"
                  type="button"
                  onClick={() => setBankModalOpen(false)}
                >
                  Cancel
                </Button>
                <Button variant="primary" className="flex-1" type="submit">
                  Create Bank
                </Button>
              </div>
            </form>
          </motion.div>
        </div>
      )}

      {/* Question Add/Edit Modal */}
      {questionModalOpen && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 overflow-y-auto py-8">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-card border border-border rounded-2xl p-6 max-w-xl w-full mx-4 shadow-soft my-auto"
          >
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-display font-bold text-foreground text-lg">
                {editingQuestion ? 'Edit Question' : 'Add Question'}
              </h3>
              <button onClick={() => setQuestionModalOpen(false)} className="text-muted-foreground hover:text-foreground">
                <X className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleSaveQuestion} className="space-y-4">
              <Textarea
                label="Question Text *"
                placeholder="Enter the question statement..."
                value={questionFormData.questionText}
                onChange={(e) => setQuestionFormData({ ...questionFormData, questionText: e.target.value })}
                required
              />
              <div className="grid grid-cols-3 gap-3">
                <Select
                  label="Difficulty"
                  value={questionFormData.difficulty}
                  onChange={(e) => setQuestionFormData({ ...questionFormData, difficulty: e.target.value })}
                  options={[
                    { value: 'EASY', label: 'Easy' },
                    { value: 'MEDIUM', label: 'Medium' },
                    { value: 'HARD', label: 'Hard' }
                  ]}
                />
                <Select
                  label="Question Type"
                  value={questionFormData.questionType}
                  onChange={(e) => setQuestionFormData({ ...questionFormData, questionType: e.target.value })}
                  options={[
                    { value: 'MCQ', label: 'MCQ' },
                    { value: 'TRUE_FALSE', label: 'True / False' }
                  ]}
                />
                <Input
                  label="Marks"
                  type="number"
                  value={questionFormData.marks}
                  onChange={(e) => setQuestionFormData({ ...questionFormData, marks: e.target.value })}
                />
              </div>

              {questionFormData.questionType === 'MCQ' && (
                <div className="space-y-2">
                  <label className="text-xs font-semibold text-muted-foreground uppercase">Options *</label>
                  {questionFormData.options.map((opt, idx) => (
                    <div key={idx} className="flex gap-2">
                      <span className="h-10 w-10 border border-border rounded-lg bg-muted/40 flex items-center justify-center font-bold text-sm text-muted-foreground">
                        {String.fromCharCode(65 + idx)}
                      </span>
                      <Input
                        placeholder={`Option ${String.fromCharCode(65 + idx)}`}
                        value={opt}
                        onChange={(e) => {
                          const updated = [...questionFormData.options];
                          updated[idx] = e.target.value;
                          setQuestionFormData({ ...questionFormData, options: updated });
                        }}
                        className="flex-1"
                        required
                      />
                    </div>
                  ))}
                </div>
              )}

              {questionFormData.questionType === 'TRUE_FALSE' ? (
                <Select
                  label="Correct Answer *"
                  value={questionFormData.correctAnswer}
                  onChange={(e) => setQuestionFormData({ ...questionFormData, correctAnswer: e.target.value })}
                  required
                  placeholder="Select true/false"
                  options={[
                    { value: 'True', label: 'True' },
                    { value: 'False', label: 'False' }
                  ]}
                />
              ) : (
                <Select
                  label="Correct Answer *"
                  value={questionFormData.correctAnswer}
                  onChange={(e) => setQuestionFormData({ ...questionFormData, correctAnswer: e.target.value })}
                  required
                  placeholder="Select correct option value"
                  options={questionFormData.options.filter(o => o.trim() !== '').map((o) => ({
                    value: o,
                    label: o
                  }))}
                />
              )}

              <Textarea
                label="Explanation"
                placeholder="Explain the logic behind the correct answer..."
                value={questionFormData.explanation}
                onChange={(e) => setQuestionFormData({ ...questionFormData, explanation: e.target.value })}
              />

              <div className="flex gap-3 pt-2">
                <Button
                  variant="outline"
                  className="flex-1"
                  type="button"
                  onClick={() => setQuestionModalOpen(false)}
                >
                  Cancel
                </Button>
                <Button variant="primary" className="flex-1" type="submit">
                  Save Question
                </Button>
              </div>
            </form>
          </motion.div>
        </div>
      )}

      {/* Delete Bank confirmation */}
      <ConfirmDialog
        open={confirmDeleteBankOpen}
        onClose={() => setConfirmDeleteBankOpen(false)}
        onConfirm={handleDeleteBank}
        title="Delete Question Bank?"
        description="Are you sure you want to delete this question bank? This will delete all questions stored in it. This action is irreversible."
        confirmLabel="Delete"
        variant="danger"
      />

      {/* Delete Question confirmation */}
      <ConfirmDialog
        open={confirmDeleteQuestionOpen}
        onClose={() => setConfirmDeleteQuestionOpen(false)}
        onConfirm={handleDeleteQuestion}
        title="Delete Question?"
        description="Are you sure you want to delete this question? This action cannot be undone."
        confirmLabel="Delete"
        variant="danger"
      />
    </div>
  );
}
